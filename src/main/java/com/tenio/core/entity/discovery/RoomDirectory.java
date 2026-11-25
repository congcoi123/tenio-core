/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.entity.discovery;

import com.tenio.core.entity.Room;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Explicitly published public room summaries indexed by group. Queries never scan unrelated groups.
 * Applications supply authenticated subscription identities and encode pages/deltas using their
 * negotiated protocol. Passwords, internal properties and arbitrary Room fields are never copied.
 */
public final class RoomDirectory implements AutoCloseable {
  /** Public room data, detached from mutable Room objects. */
  public record Summary(
      long id,
      String name,
      String group,
      String gameType,
      String state,
      int availableSlots,
      boolean active,
      Map<String, Object> attributes) {
    public Summary {
      attributes = Map.copyOf(attributes);
    }
  }

  /** Bounded equality query; all custom attributes must have been allowlisted at construction. */
  public record Query(
      String group,
      String gameType,
      String state,
      int minimumSlots,
      Map<String, Object> attributes,
      int offset,
      int limit) {
    public Query {
      Objects.requireNonNull(group);
      attributes = Map.copyOf(attributes);
      if (group.isBlank() || minimumSlots < 0 || offset < 0 || limit < 1 || limit > 100) {
        throw new IllegalArgumentException("Invalid room query; page limit is 1–100");
      }
    }

    /** Creates an unfiltered first page for one group. */
    public static Query group(String group) {
      return new Query(group, null, null, 0, Map.of(), 0, 50);
    }
  }

  /** Versioned query result. A changed revision between pages requires restarting pagination. */
  public record Page(long revision, int total, List<Summary> rooms) {
    public Page {
      rooms = List.copyOf(rooms);
    }
  }

  /** Subscription delta; RESET requires querying a new snapshot and discarding older deltas. */
  public record Change(long revision, Kind kind, long roomId, Summary summary) {}

  /** Public directory event types. */
  public enum Kind {
    /** Newly visible room. */
    ADD,
    /** Changed room summary. */
    UPDATE,
    /** Room no longer visible in this group. */
    REMOVE,
    /** Bounded delta queue overflowed; obtain a fresh page. */
    RESET
  }

  private static final int MAX_ATTRIBUTES = 16;
  private static final int MAX_ATTRIBUTE_TEXT = 256;
  private static final int SUBSCRIPTIONS_PER_IDENTITY = 4;
  private static final int DELTA_CAPACITY = 128;
  private final Set<String> fields;
  private final int maximumRooms;
  private final int maximumSubscriptions;
  private final Map<Long, Summary> rooms = new HashMap<>();
  private final Map<String, LinkedHashMap<Long, Summary>> groups = new HashMap<>();
  private final Set<Subscription> subscriptions = new java.util.HashSet<>();
  private long revision;
  private boolean closed;

  /**
   * Creates an empty directory. Rooms remain private until publish() is called.
   *
   * @param fields allowlisted public/searchable application attribute names, at most 16
   * @param maximumRooms maximum published summaries
   * @param maximumSubscriptions total active subscriptions
   */
  public RoomDirectory(Set<String> fields, int maximumRooms, int maximumSubscriptions) {
    this.fields = Set.copyOf(fields);
    if (fields.size() > MAX_ATTRIBUTES
        || maximumRooms < 1
        || maximumSubscriptions < 1
        || fields.stream().anyMatch(f -> f.isBlank() || f.length() > 64)) {
      throw new IllegalArgumentException("Invalid directory bounds or fields");
    }
    this.maximumRooms = maximumRooms;
    this.maximumSubscriptions = maximumSubscriptions;
  }

  /** Publishes only selected room data and explicitly supplied safe scalar attributes. */
  public void publish(
      Room room, String group, String gameType, String state, Map<String, Object> attributes) {
    var summary = capture(room, group, gameType, state, attributes);
    synchronized (this) {
      apply(summary);
    }
  }

  private Summary capture(
      Room room, String group, String gameType, String state, Map<String, Object> attributes) {
    Objects.requireNonNull(room);
    text(group);
    text(gameType);
    text(state);
    validateAttributes(attributes);
    // Custom Room getters may own application locks; never invoke them under the directory lock.
    String name = room.getName();
    if (name == null || name.length() > MAX_ATTRIBUTE_TEXT) {
      throw new IllegalArgumentException("Room name exceeds public summary bound");
    }
    return new Summary(
        room.getId(),
        name,
        group,
        gameType,
        state,
        Math.max(0, room.getMaxParticipants() - room.getSnapshotParticipantCount()),
        room.isActivated(),
        attributes);
  }

  private void apply(Summary summary) {
    requireOpen();
    if (!rooms.containsKey(summary.id()) && rooms.size() >= maximumRooms) {
      throw new IllegalStateException("Published room capacity reached");
    }
    Summary old = rooms.get(summary.id());
    if (summary.equals(old)) {
      return;
    }
    if (old != null && !old.group().equals(summary.group())) {
      remove(summary.id());
    }
    rooms.put(summary.id(), summary);
    groups
        .computeIfAbsent(summary.group(), ignored -> new LinkedHashMap<>())
        .put(summary.id(), summary);
    emit(
        summary.group(),
        new Change(
            ++revision,
            old == null || !old.group().equals(summary.group()) ? Kind.ADD : Kind.UPDATE,
            summary.id(),
            summary));
  }

  /**
   * Refreshes a published summary without resurrecting a concurrently removed or republished room.
   */
  public void refresh(Room room) {
    long id = room.getId();
    Summary old;
    synchronized (this) {
      old = rooms.get(id);
      if (closed || old == null) {
        return;
      }
    }
    var summary = capture(room, old.group(), old.gameType(), old.state(), old.attributes());
    synchronized (this) {
      if (!closed && rooms.get(id) == old) {
        apply(summary);
      }
    }
  }

  /** Removes a published room and emits a group-local removal delta. */
  public synchronized void remove(long roomId) {
    Summary old = rooms.remove(roomId);
    if (old == null) {
      return;
    }
    var group = groups.get(old.group());
    group.remove(roomId);
    if (group.isEmpty()) {
      groups.remove(old.group());
    }
    emit(old.group(), new Change(++revision, Kind.REMOVE, roomId, null));
  }

  /**
   * Returns a bounded page by inspecting at most the configured room bound in the requested group.
   */
  public synchronized Page query(Query query) {
    requireOpen();
    validateAttributes(query.attributes());
    var found = new ArrayList<Summary>();
    int total = 0;
    for (Summary room : groups.getOrDefault(query.group(), new LinkedHashMap<>()).values()) {
      if ((query.gameType() == null || query.gameType().equals(room.gameType()))
          && (query.state() == null || query.state().equals(room.state()))
          && room.availableSlots() >= query.minimumSlots()
          && query.attributes().entrySet().stream()
              .allMatch(e -> Objects.equals(room.attributes().get(e.getKey()), e.getValue()))) {
        if (total >= query.offset() && found.size() < query.limit()) {
          found.add(room);
        }
        total++;
      }
    }
    return new Page(revision, total, found);
  }

  /**
   * Subscribes one authenticated identity to a group. Obtain an initial versioned page through
   * query() and discard polled deltas at or before its revision. No callback runs inside this lock.
   */
  public synchronized Subscription subscribe(String identity, String group) {
    requireOpen();
    text(identity);
    text(group);
    long owned = subscriptions.stream().filter(s -> s.identity.equals(identity)).count();
    if (subscriptions.size() >= maximumSubscriptions || owned >= SUBSCRIPTIONS_PER_IDENTITY) {
      throw new IllegalStateException("Room subscription capacity reached");
    }
    var subscription = new Subscription(identity, group);
    subscriptions.add(subscription);
    return subscription;
  }

  /** Releases all group subscriptions for a logical player, including retained recovery state. */
  public synchronized void release(String identity) {
    for (var s : List.copyOf(subscriptions)) {
      if (s.identity.equals(identity)) {
        s.close();
      }
    }
  }

  /** A bounded, pull-based delta stream. Close on logout or when leaving the lobby. */
  public final class Subscription implements AutoCloseable {
    private final String identity;
    private final String group;
    private final ArrayDeque<Change> pending = new ArrayDeque<>();
    private boolean reset;
    private boolean ended;

    private Subscription(String identity, String group) {
      this.identity = identity;
      this.group = group;
    }

    /**
     * @return next ordered delta, or null; RESET means the client must obtain a fresh page
     */
    public Change poll() {
      synchronized (RoomDirectory.this) {
        return pending.poll();
      }
    }

    /**
     * Acknowledges a refreshed snapshot cursor. Updates newer than that snapshot are preserved.
     *
     * @param cursor revision returned by query()
     */
    public void acknowledgeSnapshot(long cursor) {
      synchronized (RoomDirectory.this) {
        if (cursor < 0 || cursor > revision) {
          throw new IllegalArgumentException("Invalid directory cursor");
        }
        pending.removeIf(change -> change.revision() <= cursor);
        reset = false;
      }
    }

    /** Unsubscribes and clears queued public summaries. */
    @Override
    public void close() {
      synchronized (RoomDirectory.this) {
        ended = true;
        pending.clear();
        subscriptions.remove(this);
      }
    }
  }

  private void emit(String group, Change change) {
    for (var s : subscriptions) {
      if (!s.group.equals(group) || s.ended) {
        continue;
      }
      if (s.reset || s.pending.size() >= DELTA_CAPACITY) {
        s.pending.clear();
        s.reset = true;
        s.pending.add(new Change(change.revision(), Kind.RESET, 0, null));
      } else {
        s.pending.add(change);
      }
    }
  }

  private void validateAttributes(Map<String, Object> attributes) {
    if (attributes.size() > MAX_ATTRIBUTES || !fields.containsAll(attributes.keySet())) {
      throw new IllegalArgumentException("Unpublished room query/summary attribute");
    }
    for (Object value : attributes.values()) {
      if (!(value instanceof String
              || value instanceof Boolean
              || value instanceof Integer
              || value instanceof Long
              || value instanceof Double)
          || (value instanceof String s && s.length() > MAX_ATTRIBUTE_TEXT)
          || (value instanceof Double d && !Double.isFinite(d))) {
        throw new IllegalArgumentException("Room attributes must be bounded public scalar values");
      }
    }
  }

  private static void text(String value) {
    if (value == null || value.isBlank() || value.length() > MAX_ATTRIBUTE_TEXT) {
      throw new IllegalArgumentException("Invalid directory text");
    }
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Directory closed");
    }
  }

  /** Clears summaries, indexes and all subscriptions. */
  @Override
  public synchronized void close() {
    closed = true;
    subscriptions.forEach(
        s -> {
          s.ended = true;
          s.pending.clear();
        });
    subscriptions.clear();
    rooms.clear();
    groups.clear();
  }
}
