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

package com.tenio.core.entity.replication;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit, typed, bounded replicated state. Existing entity property maps are never broadcast.
 * Applications encode snapshots/deltas using their protocol and obtain Audience from server-side
 * membership checks on every send/poll, never from client-provided flags. Scalar v1 values are
 * immutable; high-frequency simulation state belongs in the application's dedicated update path.
 */
public final class ReplicatedState implements AutoCloseable {
  /** Visibility is independent of write permissions. */
  public enum Visibility {
    /** Server-only; never present in a client snapshot/delta. */
    SERVER,
    /** Only the entity/variable owner. */
    OWNER,
    /** Members of the associated room. */
    ROOM,
    /** Authorized lobby subscribers and room members. */
    LOBBY
  }

  /** Supported scalar values have deterministic validation and bounded serialization cost. */
  public enum Type {
    /** Boolean values. */
    BOOLEAN,
    /** Signed 32-bit values. */
    INTEGER,
    /** Signed 64-bit values. */
    LONG,
    /** Finite floating-point values. */
    DOUBLE,
    /** UTF-8 strings bounded by the definition's byte limit. */
    STRING
  }

  /**
   * Immutable schema entry. Client writes are owner-only and still require room membership.
   *
   * @param type required Java scalar type
   * @param visibility client read scope
   * @param clientWritable whether the authenticated owner may update the value
   * @param maximumBytes positive UTF-8/value bound
   */
  public record Definition(
      Type type, Visibility visibility, boolean clientWritable, int maximumBytes) {
    public Definition {
      Objects.requireNonNull(type);
      Objects.requireNonNull(visibility);
      if (maximumBytes < 1
          || maximumBytes > 65536
          || (visibility == Visibility.SERVER && clientWritable)) {
        throw new IllegalArgumentException("Invalid state field bounds/permissions");
      }
    }
  }

  /** Trusted server-computed context, evaluated again when delivering a queued change. */
  public record Audience(String identity, boolean roomMember, boolean lobbySubscriber) {
    public Audience {
      Objects.requireNonNull(identity);
    }
  }

  /** Immutable initial state visible to one audience at an exact revision. */
  public record Snapshot(long revision, Map<String, Object> values) {
    public Snapshot {
      values = Map.copyOf(values);
    }
  }

  /**
   * Atomic batch delta. A resync marker requires a fresh snapshot; it contains no values. Empty
   * filtered deltas still advance the public cursor without revealing hidden values.
   */
  public record Change(
      long revision, Map<String, Object> values, Set<String> removed, boolean resync) {
    public Change {
      values = Map.copyOf(values);
      removed = Set.copyOf(removed);
    }
  }

  private static final int MAX_FIELDS = 64;
  private static final int DELTA_CAPACITY = 128;
  private final String owner;
  private final boolean playerScope;
  private final Map<String, Definition> schema;
  private final int maximumBytes;
  private final int maximumSubscribers;
  private final Map<String, Object> values = new HashMap<>();
  private final Set<Subscription> subscriptions = new HashSet<>();
  private long revision;
  private boolean closed;

  /**
   * Creates empty explicitly replicated state. A null room owner means server writes only.
   *
   * @param owner authenticated owner identity, or null for server-owned room state
   * @param playerScope true for player state; its owner may write outside a room
   * @param schema public and private field definitions; at most 64
   * @param maximumBytes total stored field names and values, including server-only fields
   * @param maximumSubscribers active subscribers bound
   */
  public ReplicatedState(
      String owner,
      boolean playerScope,
      Map<String, Definition> schema,
      int maximumBytes,
      int maximumSubscribers) {
    this.owner = owner;
    this.playerScope = playerScope;
    this.schema = Map.copyOf(schema);
    if (schema.isEmpty()
        || schema.size() > MAX_FIELDS
        || maximumBytes < 1
        || maximumBytes > 1024 * 1024
        || maximumSubscribers < 1
        || schema.keySet().stream()
            .anyMatch(
                name -> name.isBlank() || name.getBytes(StandardCharsets.UTF_8).length > 64)) {
      throw new IllegalArgumentException("Invalid replicated state schema/bounds");
    }
    this.maximumBytes = maximumBytes;
    this.maximumSubscribers = maximumSubscribers;
  }

  /**
   * Applies an atomic server-authoritative batch. Null values delete the named fields.
   *
   * @param updates field changes; all are validated before committing
   * @param expectedRevision optimistic cursor, or -1 for an unconditional server update
   * @return committed revision; unchanged batches retain the prior revision
   */
  public synchronized long updateServer(Map<String, Object> updates, long expectedRevision) {
    return update(null, updates, expectedRevision);
  }

  /**
   * Applies an owner-authorized client batch. Every field must explicitly permit client writes.
   *
   * @param actor authenticated identity and current server-computed membership
   * @param updates batch; null values delete fields
   * @param expectedRevision required current client cursor, never -1
   * @return committed revision
   */
  public synchronized long updateClient(
      Audience actor, Map<String, Object> updates, long expectedRevision) {
    Objects.requireNonNull(actor);
    if (expectedRevision < 0) {
      throw new IllegalArgumentException("Client revision required");
    }
    if (!Objects.equals(owner, actor.identity()) || (!playerScope && !actor.roomMember())) {
      throw new SecurityException("Only the active entity owner may update client-writable fields");
    }
    return update(actor, updates, expectedRevision);
  }

  private long update(Audience actor, Map<String, Object> updates, long expectedRevision) {
    requireOpen();
    if (expectedRevision < -1 || (expectedRevision != -1 && expectedRevision != revision)) {
      throw new IllegalStateException("State revision conflict; obtain a fresh snapshot");
    }
    if (updates.size() > MAX_FIELDS) {
      throw new IllegalArgumentException("Too many state fields");
    }
    var next = new HashMap<>(values);
    var changed = new LinkedHashMap<String, Object>();
    var removed = new HashSet<String>();
    for (var update : updates.entrySet()) {
      Definition field = schema.get(update.getKey());
      if (field == null) {
        throw new IllegalArgumentException("Unknown replicated state field");
      }
      if (actor != null && !field.clientWritable()) {
        throw new SecurityException("Server-authoritative field");
      }
      Object value = update.getValue();
      if (value == null) {
        if (next.remove(update.getKey()) != null) {
          removed.add(update.getKey());
        }
      } else {
        validate(field, value);
        if (!Objects.equals(next.put(update.getKey(), value), value)) {
          changed.put(update.getKey(), value);
        }
      }
    }
    long size = 0;
    for (var item : next.entrySet()) {
      size += item.getKey().getBytes(StandardCharsets.UTF_8).length + bytes(item.getValue());
    }
    if (size > maximumBytes) {
      throw new IllegalArgumentException("Replicated state byte bound exceeded");
    }
    if (changed.isEmpty() && removed.isEmpty()) {
      return revision;
    }
    values.clear();
    values.putAll(next);
    var change = new Change(++revision, changed, removed, false);
    for (var subscription : subscriptions) {
      if (subscription.reset || subscription.pending.size() >= DELTA_CAPACITY) {
        subscription.pending.clear();
        subscription.reset = true;
        subscription.pending.add(new Change(revision, Map.of(), Set.of(), true));
      } else {
        subscription.pending.add(change);
      }
    }
    return revision;
  }

  /** Captures an immutable snapshot filtered for the current server-authorized audience. */
  public synchronized Snapshot snapshot(Audience audience) {
    requireOpen();
    return new Snapshot(revision, filter(values, audience));
  }

  /**
   * Subscribes an authenticated identity. Call subscription.snapshot() before polling deltas.
   * Snapshots and subscription registration use the same lock, eliminating the initial-state gap.
   */
  public synchronized Subscription subscribe(String identity) {
    requireOpen();
    Objects.requireNonNull(identity);
    if (subscriptions.size() >= maximumSubscribers) {
      throw new IllegalStateException("Subscriber bound reached");
    }
    var subscription = new Subscription(identity);
    subscriptions.add(subscription);
    return subscription;
  }

  /** Bounded cursor stream. Membership is checked from a fresh Audience on every delivery. */
  public final class Subscription implements AutoCloseable {
    private final String identity;
    private final ArrayDeque<Change> pending = new ArrayDeque<>();
    private boolean reset;
    private boolean initialized;
    private boolean ended;

    private Subscription(String identity) {
      this.identity = identity;
    }

    /** Replaces queued deltas with a consistent current snapshot at the returned revision. */
    public Snapshot snapshot(Audience audience) {
      synchronized (ReplicatedState.this) {
        verify(audience);
        var snapshot = ReplicatedState.this.snapshot(audience);
        pending.clear();
        reset = false;
        initialized = true;
        return snapshot;
      }
    }

    /** Returns a filtered delta, null when caught up, or a resync marker after overflow. */
    public Change poll(Audience audience) {
      synchronized (ReplicatedState.this) {
        verify(audience);
        if (!initialized) {
          throw new IllegalStateException("Initial snapshot required");
        }
        var change = pending.poll();
        if (change == null || change.resync()) {
          return change;
        }
        var removed = new HashSet<String>();
        for (String name : change.removed()) {
          if (visible(schema.get(name), audience)) {
            removed.add(name);
          }
        }
        return new Change(change.revision(), filter(change.values(), audience), removed, false);
      }
    }

    private void verify(Audience audience) {
      requireOpen();
      if (ended || !identity.equals(audience.identity())) {
        throw new SecurityException("Subscription identity mismatch");
      }
    }

    /** Removes the subscriber and discards queued changes. */
    @Override
    public void close() {
      synchronized (ReplicatedState.this) {
        ended = true;
        pending.clear();
        subscriptions.remove(this);
      }
    }
  }

  /** Removes subscriptions for a logical player during final logout. */
  public synchronized void release(String identity) {
    for (var subscription : List.copyOf(subscriptions)) {
      if (subscription.identity.equals(identity)) {
        subscription.close();
      }
    }
  }

  private Map<String, Object> filter(Map<String, Object> source, Audience audience) {
    Objects.requireNonNull(audience);
    var result = new LinkedHashMap<String, Object>();
    source.forEach(
        (key, value) -> {
          if (visible(schema.get(key), audience)) {
            result.put(key, value);
          }
        });
    return result;
  }

  private boolean visible(Definition field, Audience audience) {
    return switch (field.visibility()) {
      case SERVER -> false;
      case OWNER -> Objects.equals(owner, audience.identity());
      case ROOM -> audience.roomMember();
      case LOBBY -> audience.roomMember() || audience.lobbySubscriber();
    };
  }

  private static void validate(Definition field, Object value) {
    boolean valid =
        switch (field.type()) {
          case BOOLEAN -> value instanceof Boolean;
          case INTEGER -> value instanceof Integer;
          case LONG -> value instanceof Long;
          case DOUBLE -> value instanceof Double d && Double.isFinite(d);
          case STRING -> value instanceof String;
        };
    if (value instanceof String text && text.length() > field.maximumBytes()) {
      throw new IllegalArgumentException("State text exceeds its UTF-8 bound");
    }
    if (!valid || bytes(value) > field.maximumBytes()) {
      throw new IllegalArgumentException("Invalid state type/size");
    }
  }

  private static int bytes(Object value) {
    if (value instanceof String s) {
      return s.getBytes(StandardCharsets.UTF_8).length;
    }
    if (value instanceof Boolean) {
      return 1;
    }
    return value instanceof Integer ? 4 : 8;
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Replicated state closed");
    }
  }

  /** Releases values and all subscribers. Further operations fail explicitly. */
  @Override
  public synchronized void close() {
    closed = true;
    values.clear();
    subscriptions.forEach(
        subscription -> {
          subscription.ended = true;
          subscription.pending.clear();
        });
    subscriptions.clear();
  }
}
