# Disabled-feature benchmark

This is a synthetic comparison, not a full-server load test or a performance guarantee.

| Path | Baseline ns/op | Candidate ns/op | Baseline bytes/op | Candidate bytes/op |
| --- | ---: | ---: | ---: | ---: |
| packet-creation | 34.517 | 34.913 | 64.0 | 64.0 |
| fifo | 34.050 | 33.541 | 24.0 | 24.0 |
| inline | 2.398 | 2.442 | 0.0 | 0.0 |

## Method

- Baseline: the uploaded `tenio-core(20261123-184744).zip`; its original ClientCommandManager, OutboundQueueImpl and PacketImpl were compiled separately.
- Candidate: the modified classes. Both variants used the same harness, remaining core classes and dependency classpath to isolate these paths.
- JDK: Eclipse Temurin 21.0.13+2 on the shared Linux execution host; heap `-Xms256m -Xmx256m`.
- Five fresh JVM forks per variant, alternating variant order. Four warmup rounds and seven measured rounds per path, two million operations each. The table reports the median of the per-fork medians.
- FIFO operation: enqueue, peek, dequeue with no delivery metadata or diagnostics. Packet creation allocates an escaping empty packet. Inline dispatch uses an unannotated handler with monitoring disabled.
- Allocation counts use ThreadMXBean for the benchmark thread. All forks verified that neither command-dispatch workers nor a performance exporter existed in disabled mode.

## Interpretation

The candidate retained the same measured allocation rates on these paths, including packet creation (its optional metadata reference fits existing object padding on this JVM). Timing differences are small in absolute terms and sensitive to JIT, GC and shared-host scheduling. These results cannot establish zero overhead or predict a game server workload. Fixed server/queue object fields and enabled-feature work are outside this per-operation allocation comparison.

A production acceptance run should measure throughput, p95/p99 latency, GC, memory and CPU under the actual TCP/UDP/WebSocket workload. Client recovery and lobby/replication adapters need their own load and fault tests.

## Run the benchmark

The runnable harness is part of the test sources:
[`DisabledFeatureBenchmark`](../../src/test/java/com/tenio/core/benchmark/DisabledFeatureBenchmark.java).
It has a `main` method and is not a JUnit test, so normal `mvn test` runs compile it without running
the workload or asserting timing thresholds.

### IntelliJ IDEA

Open `src/test/java/com/tenio/core/benchmark/DisabledFeatureBenchmark.java` and run its `main`
method using the **tenio-core test classpath** and Java 21. Set VM options to
`-Xms256m -Xmx256m` for comparison with the recorded measurements. Run each fork as a new process.

### Maven and command line

From the tenio-core repository root, compile the test sources and write the dependency classpath:

```sh
mvn test-compile dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/benchmark-classpath.txt -Dgpg.skip=true
```

Linux/macOS:

```sh
java -Xms256m -Xmx256m -cp "target/test-classes:target/classes:$(cat target/benchmark-classpath.txt)" com.tenio.core.benchmark.DisabledFeatureBenchmark
```

Windows PowerShell:

```powershell
$benchmarkClasspath = "target\test-classes;target\classes;" + (Get-Content target/benchmark-classpath.txt -Raw).Trim()
java -Xms256m -Xmx256m -cp $benchmarkClasspath com.tenio.core.benchmark.DisabledFeatureBenchmark
```

The output reports `ns/op` and `bytes/op` for packet creation, FIFO queue operations and inline
command dispatch. One invocation includes four warmup rounds and seven measured rounds per path;
it reports the median of the measured rounds. Repeat in at least five fresh JVMs before comparing
results. This synthetic harness does not measure the full server, receive routing, network latency,
recovery, discovery or state replication.

For a before/after comparison, build baseline and candidate with the same JDK. Compile the harness
once, then launch it with the corresponding framework classes first on each process's classpath.
Keep heap, host load and JVM flags fixed. The table above is the earlier recorded comparison; moving
the harness does not replace those results. Raw per-fork values are in `disabled-results.json`.
