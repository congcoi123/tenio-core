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

package com.tenio.core.recovery;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataType;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Opt-in, process-local recovery authority with single-use tokens and bounded replay storage.
 * Enrollment follows application authentication. Process restart never restores credentials.
 */
public final class RecoveryManager implements AutoCloseable {
  /** Immutable encoded message; both construction and access copy the payload bytes. */
  public record Message(long sequence, DataType type, byte[] bytes, long recordedAt) {
    public Message {
      bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
      return bytes.clone();
    }
  }

  /**
   * Material for the new connection. Snapshot may be null in replay-only mode.
   *
   * @param token rotated secret, delivered only to the recovered client
   * @param sequence newest replay cursor represented by the result
   * @param snapshot snapshot bytes, or null
   * @param events ordered events after the acknowledged/snapshot cursor
   */
  public record Result(String token, long sequence, Message snapshot, List<Message> events) {
    public Result {
      events = List.copyOf(events);
    }

    @Override
    public String toString() {
      return "RecoveryResult[redacted]";
    }
  }

  /** An exclusive token claim, valid until completed or aborted. */
  public record Claim(Player player, RecoveryPolicy.Credentials credentials) {
    @Override
    public String toString() {
      return "RecoveryClaim[redacted]";
    }
  }

  private static final SecureRandom RANDOM = new SecureRandom();
  private final RecoveryPolicy policy;
  private final Clock clock;
  private final Consumer<Player> expiration;
  private final Map<String, Entry> players = new HashMap<>();
  private final Map<Player, Entry> identities = new java.util.IdentityHashMap<>();
  private final int maxPlayers;
  private final int maxReplayBytes;
  private final int maxReplayEvents;
  private final RecoveryPolicy.Strategy strategy;
  private final long graceMillis;
  private final long replayAgeMillis;
  private boolean closed;

  private static final class Entry {
    final Player player;
    final String identity;
    final ArrayDeque<Message> events = new ArrayDeque<>();
    byte[] token;
    long disconnectedAt = -1;
    long sequence;
    long discardedThrough;
    long bytes;
    Claim claim;
    long claimedAt;

    Entry(Player player, String identity, byte[] token) {
      this.player = player;
      this.identity = identity;
      this.token = token;
    }
  }

  /**
   * Creates a recovery authority with a system UTC clock.
   *
   * @param policy application handshake and delivery callbacks
   * @param expiration removes a player whose disconnected recovery window expires
   */
  public RecoveryManager(RecoveryPolicy policy, Consumer<Player> expiration) {
    this(policy, expiration, Clock.systemUTC());
  }

  /** Creates an authority with an injectable clock for deterministic expiry testing. */
  public RecoveryManager(RecoveryPolicy policy, Consumer<Player> expiration, Clock clock) {
    this.policy = Objects.requireNonNull(policy);
    this.expiration = Objects.requireNonNull(expiration);
    this.clock = Objects.requireNonNull(clock);
    graceMillis = policy.grace().toMillis();
    replayAgeMillis = policy.maxReplayAge().toMillis();
    maxPlayers = policy.maxPlayers();
    maxReplayBytes = policy.maxReplayBytes();
    maxReplayEvents = policy.maxReplayEvents();
    strategy = Objects.requireNonNull(policy.strategy());
    if (graceMillis <= 0
        || replayAgeMillis <= 0
        || maxPlayers <= 0
        || maxReplayBytes <= 0
        || maxReplayEvents <= 0) {
      throw new IllegalArgumentException("Recovery bounds must be positive");
    }
  }

  /**
   * Enrolls an authenticated player or rotates its existing token. Retained state is reset.
   *
   * @param player authenticated, logged-in player owned by this server
   * @return opaque token to send privately to that player
   */
  public String issue(Player player) {
    Objects.requireNonNull(player);
    // Player implementations can own their own locks. Read their identity before taking ours.
    String identity = player.getIdentity();
    boolean loggedIn = player.isLoggedIn();
    if (!loggedIn || identity == null) {
      throw new IllegalStateException("Recovery enrollment requires an authenticated player");
    }
    byte[] token = token();
    synchronized (this) {
      if (closed || (!players.containsKey(identity) && players.size() >= maxPlayers)) {
        throw new IllegalStateException("Recovery closed or player capacity reached");
      }
      Entry old = players.get(identity);
      if (old != null) {
        identities.remove(old.player);
      }
      var entry = new Entry(player, identity, token);
      players.put(identity, entry);
      identities.put(player, entry);
      notifyAll();
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
  }

  /**
   * Records only application-selected replayable messages. Call in game-state mutation order.
   *
   * @param player enrolled player
   * @param message detached during this call
   * @return assigned sequence, or zero when not enrolled
   */
  public long record(Player player, DataCollection message) {
    Entry enrolled;
    synchronized (this) {
      enrolled = entry(player);
      if (closed || enrolled == null) {
        return 0;
      }
    }
    // Detach custom data without holding the authority monitor. Call in application mutation order.
    var bytes = message.toBinaries();
    var type = message.getType();
    if (bytes.length > maxReplayBytes) {
      throw new IllegalArgumentException("Replay message exceeds byte bound");
    }
    synchronized (this) {
      Entry entry = entry(player);
      if (closed || entry != enrolled) {
        return 0;
      }
      long sequence = ++entry.sequence;
      if (strategy == RecoveryPolicy.Strategy.SNAPSHOT) {
        return sequence;
      }
      entry.events.add(new Message(sequence, type, bytes, clock.millis()));
      entry.bytes += bytes.length;
      prune(entry);
      while (entry.events.size() > maxReplayEvents || entry.bytes > maxReplayBytes) {
        var removed = entry.events.remove();
        entry.bytes -= removed.bytes.length;
        entry.discardedThrough = removed.sequence;
      }
      return sequence;
    }
  }

  /** Marks eligible transport failures as recoverable; explicit logout/kick/ban are excluded. */
  public synchronized boolean disconnected(Player player, ConnectionDisconnectMode reason) {
    Entry entry = entry(player);
    if (closed || entry == null) {
      return false;
    }
    if (reason == ConnectionDisconnectMode.RECONNECTION && entry.claim != null) {
      return true;
    }
    boolean eligible =
        reason == ConnectionDisconnectMode.LOST_IN_READ
            || reason == ConnectionDisconnectMode.LOST_IN_WRITTEN
            || reason == ConnectionDisconnectMode.EXCEPTION;
    if (!eligible) {
      players.remove(entry.identity);
      identities.remove(player);
      notifyAll();
      return false;
    }
    if (entry.disconnectedAt < 0) {
      entry.disconnectedAt = clock.millis();
    }
    return true;
  }

  /**
   * Claims a token exactly once. Active connections may be replaced using their valid token.
   *
   * @param credentials application handshake credentials
   * @return exclusive claim, or null for invalid/expired credentials or replay gaps
   */
  public synchronized Claim claim(RecoveryPolicy.Credentials credentials) {
    if (closed
        || credentials == null
        || credentials.identity() == null
        || credentials.token() == null
        || credentials.token().length() > 64) {
      return null;
    }
    Entry entry = players.get(credentials.identity());
    if (entry == null
        || entry.claim != null
        || entry.token == null
        || expired(entry)
        || credentials.lastSequence() < 0
        || credentials.lastSequence() > entry.sequence) {
      return null;
    }
    byte[] supplied;
    try {
      supplied = Base64.getUrlDecoder().decode(credentials.token());
    } catch (IllegalArgumentException malformed) {
      return null;
    }
    if (!MessageDigest.isEqual(entry.token, supplied)) {
      return null;
    }
    prune(entry);
    if (strategy == RecoveryPolicy.Strategy.REPLAY
        && credentials.lastSequence() < entry.discardedThrough) {
      return null;
    }
    entry.token = null;
    entry.claimedAt = clock.millis();
    entry.claim = new Claim(entry.player, credentials);
    return entry.claim;
  }

  /**
   * Completes a successful transport attachment and captures recovery material. Application
   * snapshot construction executes outside this manager's monitor.
   *
   * @param claim exclusive accepted claim
   * @return immutable result, or throws when state cannot safely be reconstructed
   */
  public Result complete(Claim claim) {
    long before;
    synchronized (this) {
      before = requireClaim(claim).sequence;
    }
    RecoveryPolicy.Snapshot snapshot =
        strategy == RecoveryPolicy.Strategy.REPLAY
            ? null
            : Objects.requireNonNull(policy.snapshot(claim.player(), before), "snapshot");
    Message encoded =
        snapshot == null
            ? null
            : new Message(
                snapshot.sequence(),
                snapshot.data().getType(),
                snapshot.data().toBinaries(),
                clock.millis());
    synchronized (this) {
      Entry entry = requireClaim(claim);
      prune(entry);
      long cursor = snapshot == null ? claim.credentials.lastSequence() : snapshot.sequence();
      if (cursor < entry.discardedThrough
          || cursor > entry.sequence
          || (strategy == RecoveryPolicy.Strategy.SNAPSHOT && cursor != entry.sequence)
          || (encoded != null && encoded.bytes.length > maxReplayBytes)) {
        throw new IllegalStateException("Recovery snapshot/replay cursor no longer covered");
      }
      var replay =
          strategy == RecoveryPolicy.Strategy.SNAPSHOT
              ? List.<Message>of()
              : entry.events.stream().filter(event -> event.sequence > cursor).toList();
      entry.token = token();
      return new Result(
          Base64.getUrlEncoder().withoutPadding().encodeToString(entry.token),
          entry.sequence,
          encoded,
          replay);
    }
  }

  /** Finishes recovery after its snapshot/replay response has been queued successfully. */
  public synchronized void finish(Claim claim) {
    Entry entry = requireClaim(claim);
    entry.claim = null;
    entry.disconnectedAt = -1;
    notifyAll();
  }

  /**
   * @return whether a token claim is currently attaching/delivering this player's state
   */
  public synchronized boolean isRecovering(Player player) {
    Entry entry = entry(player);
    return entry != null && entry.claim != null;
  }

  /**
   * Revokes a player's token and discards replay state after explicit logout or recovery failure.
   */
  public synchronized void revoke(Player player) {
    Entry entry = entry(player);
    if (entry != null) {
      players.remove(entry.identity);
      identities.remove(player);
      notifyAll();
    }
  }

  /**
   * Waits for an in-flight recovery delivery before dispatching queued inbound gameplay. Only the
   * session's virtual reader should call this; never call from a recovery callback. Wait is bounded
   * to five seconds, and interruption/failed delivery terminates admission explicitly.
   *
   * @param player player whose snapshot/replay must precede incoming gameplay
   * @return whether delivery finished and its original enrollment remains valid
   */
  public synchronized boolean awaitReady(Player player) {
    Entry original = entry(player);
    if (original == null) {
      return !closed;
    }
    long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    while (!closed && entry(player) == original && original.claim != null) {
      long remaining = until - System.nanoTime();
      if (remaining <= 0) {
        return false;
      }
      try {
        java.util.concurrent.TimeUnit.NANOSECONDS.timedWait(this, remaining);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return !closed && entry(player) == original && original.claim == null;
  }

  /** Removes expired disconnected entries and invokes cleanup outside the manager's monitor. */
  public void expire() {
    var expired = new ArrayList<Player>();
    synchronized (this) {
      players
          .values()
          .removeIf(
              entry -> {
                if ((entry.claim == null && expired(entry))
                    || (entry.claim != null && clock.millis() - entry.claimedAt >= graceMillis)) {
                  expired.add(entry.player);
                  identities.remove(entry.player);
                  return true;
                }
                return false;
              });
    }
    synchronized (this) {
      if (!expired.isEmpty()) {
        notifyAll();
      }
    }
    expired.forEach(expiration);
  }

  private void prune(Entry entry) {
    while (!entry.events.isEmpty()
        && clock.millis() - entry.events.getFirst().recordedAt >= replayAgeMillis) {
      var removed = entry.events.remove();
      entry.bytes -= removed.bytes.length;
      entry.discardedThrough = removed.sequence;
    }
  }

  private Entry requireClaim(Claim claim) {
    Entry entry = entry(claim.player());
    if (closed || entry == null || entry.claim != claim) {
      throw new IllegalStateException("Recovery claim no longer owns the player");
    }
    return entry;
  }

  private Entry entry(Player player) {
    return identities.get(player);
  }

  private boolean expired(Entry entry) {
    return entry.disconnectedAt >= 0 && clock.millis() - entry.disconnectedAt >= graceMillis;
  }

  private static byte[] token() {
    var token = new byte[32];
    RANDOM.nextBytes(token);
    return token;
  }

  /** Revokes all credentials and releases stored application state. */
  @Override
  public synchronized void close() {
    closed = true;
    players.clear();
    identities.clear();
    notifyAll();
  }
}
