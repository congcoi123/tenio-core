# Framework services

TenIO Core provides command execution, authenticated session recovery, queued delivery,
performance archives, public room discovery and typed replicated state. This guide covers their
APIs, configuration, resource bounds and integration with application protocols. Each section
explains the startup configuration or API call that activates its service. The framework preserves
existing wire formats and reserves no application operation codes.

## Command execution

```java
@ClientCommand(value = 42, name = "ready", execution = ExecutionMode.ROOM,
    mailboxCapacity = 64, requestsPerSecond = 2, burst = 4)
public final class ReadyCommand extends AbstractClientCommandHandler<Player, DataCollection> {
  @Override
  public void execute(Player player, DataCollection message) {
    // Mutate room state here. All ROOM commands for this room share a lane.
  }
}
```

Import `ExecutionMode` from `com.tenio.core.command.client`. The Injector registers the annotation
through the client command manager. Register a `ClientCommandPolicy` bean for automatic routing
from the common TCP/UDP/WebSocket receive path, or retain legacy event handling without a policy.
The startup log names the selected mode; command mode never falls back to the player-message
event. Core caches whether commands exist at startup. See the README extension/default tables.

| Mode | Contract |
| --- | --- |
| `INLINE` (default) | Existing synchronous execution on the caller. No dispatcher is created when every command is inline. |
| `SESSION` | Shared FIFO mailbox across commands using this mode for one session. |
| `ROOM` | Shared FIFO mailbox across commands using this mode for one room, including different players/handlers. Membership and a session are required. |
| `CONCURRENT` | Independent virtual-thread invocations, subject to the shared admission bound. |

The shared dispatcher admits at most 1,024 queued/running invocations and 128 active workers. A
mailbox's annotation bound includes its currently running invocation. Saturation throws
`RejectedExecutionException`; the application can send its own busy/retry response. Rate limiting
runs before dispatch; its accepted counter means rate-limit acceptance, not successful mailbox admission or handler completion. Accepted async requests receive a detached Zero/MessagePack input copy.
Queued work is skipped if its original session has closed/been replaced or its room changed.
Commands already running must cooperate with interruption and observe their own application state
versions. Inline commands, event handlers, and direct Room mutations do not automatically join a
ROOM lane. External completions must re-enter the application's mutation context before commit.

Workers stop before SERVER_TEARDOWN, discard queued requests and wait at most five seconds in
total. Blocking handlers must use I/O timeouts and release resources on interruption. Virtual threads
are not an unlimited-work admission mechanism. This feature does not implement transactions or
provide rollback for a handler that partially updates state before throwing.

## Authenticated recovery

Provide one scanned `@Component`/`@Bean` implementing `RecoveryPolicy`. Without that bean, all
existing retry/disconnection behavior is retained. A policy enables the shared TCP/WebSocket
handshake path and a one-second expiry job through the Injector-owned scheduler.

After the application's normal authentication/login succeeds:

```java
String token = api().getRecoveryManager().issue(player);
// Deliver this token privately over the authenticated encrypted connection.
```

Policy defaults are a 30-second disconnected grace period, 4,096 enrolled players, 256 replay events
and 256 KiB replay bytes per player. Replay events expire after two minutes by default. Override the policy methods to choose bounded application
values. Resource limits and strategy are captured at startup, not changed at runtime. Explicit logout, kick, ban, idle cleanup and server shutdown do not create recovery windows.
Unexpected read/write loss and connection exceptions can retain enrolled players. Enrolling a
player does not make messages durable across a process restart. Existing idle-player maintenance
can end a recovery window earlier; choose an idle timeout longer than the desired grace period.

`credentials(session, message)` returns the identity, opaque token and last applied sequence when
the handshake requests recovery, or null for the existing login/retry flow. Never return an identity
based only on a player name as proof of recovery. A present invalid/expired token closes the fresh
session; it does not fall back to a normal login. One claim wins concurrent attempts. Successful
recovery rotates the token. Failed attachment/delivery revokes the prior credential and requires a
new authenticated login.

| Strategy | Application contract |
| --- | --- |
| `SNAPSHOT` (default) | Return current authoritative state and its latest sequence. No replay event archive is retained. |
| `REPLAY` | Call `record(player, message)` for selected replayable updates in mutation order. Reject reconnect when the client's cursor predates retained coverage. |
| `SNAPSHOT_AND_REPLAY` | Capture a snapshot with the exact cursor represented in it, then replay retained messages after that cursor. |

`record` does not automatically capture all Responses, inventory transactions or property changes.
The application chooses which updates may be replayed. Do not duplicate an update in both the
snapshot and the subsequent replay. Call `RecoveryPolicy.recover` in the same room mutation context
used by state changes/broadcasts: override it to hold that context across `capture.get()` and
`resumed(...)`. The default implementation is appropriate only when the application already
serializes those operations. The application supplies a consistent snapshot/cursor pair.

The new session is attached before `resumed` delivers the snapshot, ordered replay and rotated
token. The session's virtual reader waits up to five seconds for an in-flight delivery before dispatching
queued gameplay, preserving the recovery barrier without silently discarding commands. Failed or
timed-out delivery closes the new connection. Clients must still wait for the recovery response
before sending gameplay commands. PLAYER_CONNECTION_RESUMED
is emitted after the callback completes. Existing old-session fencing remains active. An application
must serialize live broadcasts with recovery delivery to prevent newer output overtaking the
snapshot; it must not block network threads indefinitely waiting for external services.

Client adapter requirements: privately retain the current token, track the last applied replay
cursor, send recovery credentials after transport reconnection, apply snapshot before replay,
ignore duplicate sequences, and replace the old token only from a successful recovery response.
The server does not automatically reconnect client sockets or guarantee exactly-once application
side effects. Tokens require an encrypted transport; adding a policy does not enable TLS itself.

## Queued delivery

```java
response().setContent(position).setRecipientPlayer(player)
    .delivery(DeliveryOptions.replaceable("position:" + entityId, Duration.ofMillis(150)))
    .write();
```

`DeliveryOptions` holds mode, congestion priority, replacement key and queued lifetime. Factories
select HIGH for `important()`, NORMAL for `replaceable(key, lifetime)` and LOW for
`bestEffort(lifetime)`. Use `withPriority(DeliveryOptions.Priority.HIGH)` to override only priority.
Without delivery options, the default queue policy treats UDP as LOW and other transports as
NORMAL. Explicit delivery priority overrides either default, including for UDP. LOW is rejected
at 75% current occupancy, NORMAL at 90%, and HIGH remains eligible until full. Queued UDP is not evicted and FIFO order
is preserved. This reserves headroom for TCP in the shared session queue. Current direct Netty
WebSocket writes bypass these queue rules. Priority does not reorder FIFO output, bypass hard packet/byte bounds or override a
custom OutboundQueuePolicy. Explicitly lowering important-message priority also lowers its admission
eligibility. None of these settings acknowledges delivery or provides durable storage.

BEST_EFFORT drops on capacity admission failure. Queue-policy rejections remain explicit and are
counted separately from full-queue drops. The network writer handles policy drops silently; full
queues produce at most one concise WARN per 30 seconds for each standard session/direction.
Inbound full queues use the same rate limit. Expected rejection warnings omit stack traces, while
unexpected failures retain normal error reporting. Direct queue calls retain their exception
contracts. Never use replaceable/best-effort for purchases or final
match results.

Responses with no delivery metadata use implicit NORMAL priority for non-UDP packets; UDP still
uses the 75% cutoff. Both retain their allocation-free
basic queue path. The first explicitly
annotated packet activates enhanced queue bookkeeping and a default 1 MiB serialized-payload byte
bound. Packet count limits and existing queue policies remain effective. Customize per session:

```java
session.fetchOutboundQueue().configureDeliveryByteLimit(512 * 1024L);
```

Zero explicitly disables the byte bound. Byte counts measure payload size at admission, not exact
TLS/framing/compression overhead. Packets must not be mutated after enqueue. Replacement removes
one unclaimed update with the same key and transport and appends the newer one, preserving FIFO
among retained important events. A replacement rejected by policy restores the prior packet at
its original position. Expiry uses monotonic time. Claimed, fragmented and final-close packets
are protected. Data already handed to a socket cannot be replaced or expired.

Only `monitor` enables the extra reason counters: expired updates, superseded updates and
best-effort capacity drops. Required queued-byte admission bookkeeping is independent of diagnostics.
TCP queues use these options. The current WebSocket path delegates to Netty directly and does not
apply the application OutboundQueue policy; the options must not be relied upon for WebSocket output.

## Performance archives and native charts

```xml
<!-- Top-level Server/Properties -->
<Property name="profile">default,monitor,performance</Property>
```

`performance` is independent of `metrics` and `history`. The Injector selects `PerformanceJob`; its
five-second cadence is a private annotation constant, not an XML scheduler entry. Samples cover
players, rooms, sessions, process CPU, heap, cumulative GC time, byte rates, cumulative drop counts
outstanding opted-in commands, lifetime execution failure counts, and coarse command execution p95/maximum durations. Rates use actual elapsed monotonic time. Command timing is enabled only by monitor/performance; it uses a fixed 64-bucket histogram. Its p95 is a power-of-two upper bound over completed calls since monitoring began, not a rolling exact percentile. The first rate sample
is zero because no prior sample exists. Unsupported process CPU is recorded as -1.

TSV exports go under `logs/performance/`, carry UTC epoch timestamps and a unique boot ID, rotate
at approximately 8 MiB, and retain at most seven files. The directory is exclusively locked while
its exporter is active; separate JVMs require separate working/output directories. The bounded
export queue contains at most 256 samples. I/O failures or overflow increment lost-export counts.
Graceful shutdown drains for at most five seconds; an abrupt process kill can lose pending samples.
Archive retention is file-count/size based, not a guarantee of seven days of history.

With a graphical `monitor`, a 720-sample ring is available and the native window adds
**Performance history**. Choose a metric and range, then Load. Queries run off the Swing event thread;
there is no additional polling timer. UTC files display in the window's local time zone. Boot changes
break plotted lines. Large ranges preserve CPU peaks in each bucket and the latest sample, so
other metrics at those selected points are approximate rather than averages or guaranteed peaks.

The `performance [minutes]` command accepts 1–10,080 minutes and returns at most 50 historical points.
The Java query API permits at most 2,000 points and retains only bounded bucket state while reading.
Without the graphical window, no recent-sample ring is allocated. Without `performance`, no recorder,
export thread, archive or sampling job is created. Operational `history` continues to record events
independently, with its existing storage behavior.

## Public room discovery

```java
var directory = api().enableRoomDiscovery(Set.of("rank"), 10_000, 1_000);
directory.publish(room, "pong", "duel", "waiting", Map.of("rank", 3));
var page = directory.query(RoomDirectory.Query.group("pong"));
```

Call enable once during initialization. Nothing is published automatically. Summary fields are ID,
name, group, game type, application state, available participant slots, active flag and explicitly
allowlisted scalar attributes. Room passwords, owner secrets and existing property maps are not
copied. Custom searchable values are bounded String/Boolean/Integer/Long/finite Double values.
Groups index the catalog; queries inspect only the selected group, bound custom fields to 16 and
page size to 100. Store totals and cursors in the client; if revision changes between pages, restart
pagination. Summary visibility does not authorize joining a private room.

```java
var subscription = directory.subscribe(authenticatedIdentity, "pong");
var initial = directory.query(RoomDirectory.Query.group("pong"));
// Send initial page; ignore deltas <= initial.revision().
subscription.acknowledgeSnapshot(initial.revision());
var change = subscription.poll(); // null when caught up
```

Applications authorize access to groups and schedule bounded polling/delivery using their own
handlers/jobs. There is no thread or callback per subscription. Each identity may own four
subscriptions, each with at most 128 pending deltas. Overflow returns RESET, never an apparently
complete but missing update stream. Re-query then acknowledge the new snapshot revision. Changes
newer than the acknowledged revision are retained, including another RESET if the subscriber
fell behind again. Send identifiers/fields with an application-defined versioned envelope.

ServerApi joins/leaves, RoomManager renames/capacity changes and removals refresh/remove published
entries. Direct Room mutations and application state changes require `directory.refresh(room)` or
`publish(...)`. Final logout removes subscriptions. Recoverable disconnection retains them only
within the bounded recovery/player lifetime. Close an individual subscription when leaving a lobby.

## Typed replicated state

```java
var replication = api().enableStateReplication(10_000, 64 * 1024, 64);
var schema = Map.of(
    "ready", new ReplicatedState.Definition(ReplicatedState.Type.BOOLEAN,
        ReplicatedState.Visibility.ROOM, true, 1),
    "phase", new ReplicatedState.Definition(ReplicatedState.Type.STRING,
        ReplicatedState.Visibility.LOBBY, false, 32));
var state = replication.createRoom(room.getId(), owner.getIdentity(), schema);
state.updateServer(Map.of("ready", false, "phase", "waiting"), -1);
```

The registry bounds entity count; each entity bounds stored bytes and subscriptions. Schemas have
at most 64 fields. V1 supports Boolean, Integer, Long, finite Double and bounded UTF-8 String values.
Existing local property maps remain local. Arbitrary nested objects and simulation replication are
outside this scalar state API. Null batch values delete fields. Every batch validates completely
before changing values or advancing its revision; no-op batches keep the existing revision.

| Visibility | Readers |
| --- | --- |
| SERVER | Never included in client snapshots or deltas. |
| OWNER | The owner assigned at state creation. |
| ROOM | Current room members. |
| LOBBY | Authorized lobby subscribers and current room members. |

Client writes require the authenticated owner, an explicitly client-writable field, current room
membership for room-scoped state, and an exact expected revision. Server updates can supply an exact
revision or -1. Owner identity is immutable; handle ownership transfers explicitly in application
logic rather than assuming Room ownership automatically rewrites state authorization.

```java
var audience = new ReplicatedState.Audience(player.getIdentity(), isCurrentRoomMember, isAuthorizedLobbySubscriber);
var subscription = state.subscribe(player.getIdentity());
var initial = subscription.snapshot(audience);
var change = subscription.poll(audience);
```

Compute audience flags from actual server membership/authorization on every delivery. Never accept
these flags from a client. Polling filters queued deltas again, so a former room member cannot read
ROOM values queued before leaving. Send empty filtered deltas too: they advance the global revision
without exposing hidden values. On leave/logout, clients must discard previously delivered room
state; already received data cannot be revoked. A subscription queue holds at most 128 deltas;
RESYNC requires a new snapshot before continuing. There is no per-subscription worker/timer.

Final logout closes player state and removes its subscriptions to other states. Room removal closes
room state and all its subscriptions. Recoverable disconnection retains the logical player's state.
Server shutdown closes the registry. Client adapters must apply ordered revisions, detect missing
revisions, request a fresh snapshot, and check the resync marker. State changes are not automatically
written to sockets: applications choose their envelopes, authentication and send cadence.

## Validation and rollout

Run `mvn clean verify -Dgpg.skip=true` on Java 21. GPG skip affects signing only. Include all existing
unit tests, new concurrency/boundary tests, the compile-checked Java examples and disabled-path
benchmark evidence. Integration tests exercise recovery through the common processor for TCP and
WebSocket; they are not an end-to-end test of a real remote client or TLS deployment.

Start with command routing and delivery policies, then implement the client's recovery handshake.
Use the performance profile in a test instance to inspect queue and command behavior. Room
discovery and replicated state use application-defined client envelopes; negotiate protocol
versions before sending those messages to older deployed clients. Example 13 in tenio-examples
provides a runnable TCP server/client integration; TLS and remote deployment remain application
integration checks.
