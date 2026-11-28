# Framework service validation

Java 21; existing Maven dependencies and artifact version are unchanged. Changes are intended for the next release, under Unreleased.

## Full build

```sh
mvn clean install -Dgpg.skip=true
```

Result: **BUILD SUCCESS; 1,606 tests, 0 failures, 0 errors, 0 skipped**. This includes 62 additional
test executions over the baseline suite. Checkstyle reported zero violations. Main/source/Javadoc
JARs were generated. GPG signing was skipped and no deployment/upload was performed. The existing
project emits Javadoc warnings; this work does not claim a warning-free API documentation build.

| Area | Important checks |
| --- | --- |
| TCP reader | A single client write larger than the reader buffer drains across successive selections; the regression fails on the previous selection loop (one read instead of four). |
| Routing | Exclusive command/event paths, exactly-once command dispatch, resolved payload forwarding, missing/malformed/unknown rejection, mailbox rejection isolation, stale-session fencing and cached disabled routing without registry/policy calls. |
| Execution | Shared FIFO mailboxes, cross-player/handler room bound, concurrency admission, cancellation/interruption, detached input, stale-session suppression and unchanged synchronous defaults. |
| Recovery | Single-use/identity-bound token, rotation, concurrent claims, replay count/age/byte guards, snapshots after replay gaps, expiry, readiness barrier, explicit termination, callback lock boundaries and shared TCP/WebSocket processor flow. |
| Delivery | Replacement at capacity, byte limits, original-position rollback after policy rejection, claimed/fragmented/final-packet safety, expiry, copy/deadline preservation, reference reuse, disabled diagnostics and best-effort session/network totals. |
| Performance | Profile selection, sampled counts/rates without entity scans, bounded live/file mode, export drain, retention, exclusive archive ownership and peak-preserving range queries. |
| Discovery | Group filtering/pagination, allowlisted scalar attributes, no property export, overflow reset/cursor handling, custom Room getter lock boundaries and refresh/removal races. |
| Replication | Visibility, owner writes, atomic batch validation, revision conflicts, bounded UTF-8 values, snapshot/delta continuity, fresh membership filtering, overflow resync, entity/subscriber cleanup and stale-player logout isolation. |

## Examples and native UI component

The three sources in `docs/examples/` compile against the built core classes and framework
dependencies. They are application integration examples, not a substitute for authentication,
login, membership handling or a compatible client.

The offscreen `docs/validation/PanelSmoke.java` loads an actual archive off the Swing event thread,
selects the default historical range, paints the panel and exports a PNG. The smoke run passed.
This verifies component loading/rendering in headless mode; it does not verify a complete native
window on Windows/macOS or interactions with an actual remote game client.

To reproduce compilation/rendering, first produce a dependency classpath:

```sh
mvn dependency:build-classpath -Dmdep.outputFile=target/dependency-classpath.txt
```

Bash:

```sh
mkdir -p target/example-classes
tenio_cp="target/classes:$(cat target/dependency-classpath.txt)"
javac -proc:none -cp "$tenio_cp" -d target/example-classes docs/examples/*.java docs/validation/PanelSmoke.java
java -Djava.awt.headless=true -cp "target/example-classes:$tenio_cp" com.tenio.core.admin.PanelSmoke target/performance-panel.png
```

PowerShell:

```powershell
New-Item -ItemType Directory -Force target/example-classes | Out-Null
$tenioCp = "target/classes;" + (Get-Content target/dependency-classpath.txt -Raw).Trim()
$sources = Get-ChildItem docs/examples/*.java,docs/validation/PanelSmoke.java | ForEach-Object { $_.FullName }
javac -proc:none -cp $tenioCp -d target/example-classes $sources
java -Djava.awt.headless=true -cp "target/example-classes;$tenioCp" com.tenio.core.admin.PanelSmoke target/performance-panel.png
```

## Performance evidence and limits

See [the disabled-path comparison](benchmarks/disabled-results.md) and its raw per-fork results.
Packet creation, legacy FIFO and inline dispatch retained their measured allocation rates. Their
small timing differences are not a zero-overhead guarantee or a production throughput result.
The comparison isolates three changed classes using the same remaining core/dependency classpath.

The local TCP server/client walkthrough is covered below. Remote WebSocket/TLS deployment,
sustained load, network-loss simulation and process-failover were not exercised. Recovery is process-local; it is not durable
match restoration. Queued delivery options apply to the built-in TCP/UDP queues; the current direct
Netty WebSocket output path does not acquire these queue guarantees. Discovery and replicated state use
bounded pull subscriptions with application-defined wire envelopes.

## Runnable examples and automatic routing

After installing this core build locally, `mvn clean verify` in the updated tenio-examples
completed with **5 tests, 0 failures, 0 errors, 0 skipped**, and built the assembly JAR.
Example 13 starts an isolated headless server JVM and drives actual TCP messages through the
shared client codec: login/enrollment, bounded room queries, ready deltas, stale-revision
rejection, all three delivery modes on a healthy queue, unknown/malformed/missing command
rejection, snapshot recovery, token rotation, post-recovery commands and performance-file export.
The integration walkthrough runs both with and without an additionally registered player-message
event handler. It verifies the startup COMMAND log, the mixed-registration warning, and that the
legacy handler is never invoked. Policy contract tests also verify exact short extraction and
wrong-wire-type rejection. The TCP reader regression failed against the previous selection loop
(one of four reads), then passed with the fix.

Core was verified using `mvn clean install -Dgpg.skip=true`: **1,606 tests**, zero failures/errors/
skips and zero Checkstyle violations. The optional routing state is computed before worker startup
and released at processor destruction. The disabled-path test verifies the command registry is
queried only during configuration and the policy is never invoked for subsequent event-mode
messages. The benchmark numbers above cover the framework queue/packet/command paths, not a
new measurement of the receive-path boolean check. No claim of literally zero overhead is made.
