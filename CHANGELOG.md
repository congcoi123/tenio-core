# CHANGELOG

## 0.0.1 2021-10-13 Feature release
**Initial release**
- NIO mechanism  
- Server configuration  
- Events handling  
- Player and Room management  
- Session management  
- TCP, UDP and WebSocket support  
- Server monitoring  
- Schedule management  

## 0.2.0 2022-04-20 Feature release
- Fulfilled java-docs for all classes
- Optimization
- Refactoring the project's structure
- Bugs fixed  

## 0.3.0 2022-10-13 Feature release
- Supported multiple UDP channels
- Allowed fetching available UDP channel by using round-robin mechanism
- Refactoring: renamed package "extension" to "handler"
- Allowed declaring the server address in configuration file
- Supported KCP transportation
- Bugs fixed

## 0.3.1 2022-10-21 Feature release
- Multiple data serialization methods supported (MsgPack)
- Added new events to handle connection refused cases
- WebSocket handling issues fixed
- Allowed showing server's uptime 
- Refactoring the project's structure
- Bugs fixed  

## 0.4.0 2022-11-20 Feature release
- Adapted annotations' management from the [tenio-common](https://github.com/congcoi123/tenio-common) module
- Introduces more annotations
- Supports self-defined commands
- Refactoring the project's structure
- Bugs fixed  

## 0.5.0 2023-08-30 Feature release
- Upgraded to JDK 17
- Upgraded dependencies 
- Allowed checking logging configuration before writing logs 
- configuration.xml file changed
- Introduced new annotations: @RestController and @RestMapping to work with Restful, @ClientCommand
- Added a new configuration value that allows limiting the player's IDLE time in case of non-deported state enabled
- Methods changed/updated/enhanced in classes ServerApi, Injector, ServerEvent
- Annotations enhanced: @Bean, @AutowiredQualifier
- Set names for all necessary threads
- Resolved the deadlock issue while writing packets
- Player and Room are now opened for custom classes
- Removed redundant constant, enum values
- Fixed test cases' issues
- Bugs fixed 

## 0.5.1 2023-09-03 Feature release
- Fixed bugs that stops the main thread 
- It allows removing optional schedule tasks (CcuReportTask, DeadlockScanTask, SystemMonitoringTask, TrafficCounterTask)

## 0.6.0 2024-10-09 Feature release
- Upgraded dependencies
- Fixed issues related to the "100% CPU"
- Reworked on Restful

## 0.6.1 2025-01-15 Bugfix release
- Upgraded dependencies
- Fixed several issues related to concurrency

## 0.6.2 2025-05-11 Feature release
- Fixed several issues related to concurrency
- Allowed dynamically declaring behaviors for packets processing with annotations

## 0.6.3 2025-06-01 Feature release
- Fixed several bugs
- Stopped supporting multiple UDP ports, instead it is using datagram channel cache to enhance performance
- New feature and events added: Channel which allows broadcasting to subscribed players

## 0.6.4 2025-07-01 Bugfix release
- Fixed several issues related to performance
- Temporarily disabled the Request Priority Mechanism, it could be enabled by checking this constant value AbstractController#REQUEST_PRIORITY_ENABLED

## 0.6.5 2025-08-01 Bugfix release
- Upgraded dependencies
- Fixed several performance issues related to Injector (Removed unused Reflections API)
- Fixed several performance issues related to Acceptor and Reader Engines

## 0.6.6 2025-09-01 Feature release
- Upgraded dependencies
- Provided several thread-safe methods to work with lists (compute methods)
- Removed unnecessary wrapper methods
- Reworked on writer, reader and acceptor handlers
- Supported Request Policy interface

## 0.6.7 2025-09-02 Feature release
- Fixed several bugs
- Supported Datagram Packet Policy interface
- Reworked on Binary Packet Encoder / Decoder
- Full support Codec for all Transportations

## 0.6.8 2026-03-10 Bugfix release
- Upgraded dependencies
- Fixed several bugs

## 0.6.9 2026-03-23 Bugfix release
- Supported injecting multiple interfaces into one class
- Renamed all events' main method
- Updated License

## 0.6.10 2026-03-26 Bugfix release
- Supported default embedded configurations
- Refactored inbound/outbound packages

## 0.6.11 2026-04-03 Feature release
- Upgraded dependencies
- Stopped supporting KCP transportation

## 0.6.12 2026-04-04 Feature release
- Upgraded dependencies
- Simplified setting of the configuration file

## 0.7.0 2026-04-12 Feature release
- Supports Java 21 Features
- Virtual Threads are being used in Session Inbound Process Queue and Schedulers
- It should close sessions whenever exceptions occurred
- Renamed Controller to Processor

## 0.7.1 2026-04-19 Bugfix release
- It should force to close sessions and disconnect players in some scenarios

## 0.7.2 2026-04-25 Feature release
- Fixed issues which caused the Game Server takes time to shut down

## 0.7.3 2026-05-29 Feature release
- Fixed several issues in ServerApi
- Improved the performance by offloading tasks from IO bound workers
- Used EnumMap in EventHandler to increase lookup speed
- Supported multi UDP channels
- Renamed and added new APIs

## 1.0.0 2026-11-11 Feature release
- Added startup profiles for bootstrap classes, components, factories, handlers, commands, REST mappings and field injection.
- Improved dependency injection: circular dependency handling, collision diagnostics, and instance-owned Injector and Bootstrapper state.
- Added `EventConnectionWillBeClosed` and `EventPlayerConnectionReplacing` lifecycle events.
- Made player/session replacement and outbound responses consistent with the current connection; prevented stale session requests from affecting replacement connections.
- Corrected player retention, logout, idle-time calculation, and orphan-session cleanup.
- Added response APIs and support for creating rooms with externally supplied IDs.
- Added configurable TCP main-connection keepalive scanning based on the last successful read.
- Improved subscribed-event checks, monitoring, concurrent collection access, and reader/writer lifecycle behavior.
- Updated dependencies and expanded regression tests for profiles, dependency injection, sessions, connection replacement, and cleanup.
- Corrected TCP framing to decode the fully accumulated body and treat 16-bit frame lengths as unsigned. Added regression coverage for fragmented/coalesced frames and large normal-sized frames.
- Isolated launcher-profile test logging so diagnostic assertions are independent of suite logging configuration.
- Added opt-in per-command token buckets with player/session isolation and rejection counters.
- Added read-only session, room, traffic, and outbound-pressure inspection with optional per-queue metrics.
- Added a local desktop admin window and built-in system commands gated by the monitor profile.
- Pinned admin commands and added inspection usage to help. Formatted uptime and statistics totals.
- Added readable operational history, disk archives for deployments without the monitor window, and arrow-key command recall.
- Accepted an explicitly empty XML profile for deployments using unrestricted components.
- Removed the unused JLine dependencies and the unfilled security policy template.

## Unreleased — Framework services

- Add opt-in SESSION/ROOM ordered and bounded concurrent client command execution; preserve INLINE defaults, detach async inputs, fence stale sessions/room changes, and stop admitted work before application teardown.
- Add optional command duration/failure diagnostics for monitor/performance using a bounded histogram.
- Add application-defined authenticated recovery with single-use rotating tokens, bounded grace and replay retention, snapshot/replay strategies, shared TCP/WebSocket processing, and an inbound recovery barrier.
- Add queued message delivery options for unclaimed update replacement, monotonic expiry and best-effort admission, with a default enhanced byte bound and protected in-flight/final packets. Preserve existing response defaults and custom queue policies.
- Add the optional performance profile, Injector-scheduled samples, bounded rotating TSV archives, range queries, export-loss reporting and native historical charts. No metrics task cadence is added to XML.
- Add explicitly published public room discovery, group-indexed bounded queries, versioned pages, bounded subscriptions and overflow reset signals, with final logout/room removal cleanup.
- Add explicitly registered typed player/room state, visibility scopes, owner-authorized writes, atomic revisioned batches, bounded snapshot/delta subscriptions and lifecycle cleanup. Existing entity property maps remain local.
- Add concurrency, authorization, queue safety, profile selection, archive and compatibility regressions, plus application examples and a disabled-path benchmark harness.

Application adapters and coordinated client support are required for recovery envelopes, lobby deltas
and replicated state. No existing Zero/MessagePack type codes or application operation codes change.

### Automatic client command envelopes and defaults
- Added an injected `ClientCommandPolicy` for common TCP/UDP/WebSocket command extraction after login.
- Cache command routing availability at startup. No policy or no registered commands retains the event path without per-message registry checks.
- Command mode is exclusive: missing, malformed, unknown and overloaded commands never fall back to `RECEIVED_MESSAGE_FROM_PLAYER`. Valid commands retain rate limits and execution policies.
- Log the chosen mode once at startup, with explicit warnings for mixed handlers or incomplete routing setup.
- Document extension defaults and Java interface fallbacks in README tables; add command routing regression tests.
- Fix a TCP reader selection stall exposed by burst command traffic: process selected keys instead of relying on the count of newly selected keys, and remove the second selection that could leave readiness unconsumed. Add a regression that drains one write across multiple reader iterations.

### Dispatcher test cleanup
- Wait for both active lanes and their queued invocation to complete before closing the admission-test dispatcher. This prevents expected shutdown interruption from appearing as uncaught assertion errors in IntelliJ.
- Collect worker failures and assert them on the JUnit thread so a background assertion cannot leave this test green.

### Documentation
- Integrate command execution, recovery, response delivery, performance history, room discovery and replicated state into the main framework documentation.
- Replace the separate optional-capabilities presentation with feature sections, service usage, defaults and a framework-services walkthrough in example 13.
- Rename the service guide and implementation document to `framework-services.md` and `framework-services-implementation.md`, updating their references.
- Move the runnable disabled-path benchmark into test sources, with IntelliJ, Linux/macOS and Windows PowerShell instructions; normal unit tests do not run the benchmark workload.

### Startup extension-point diagnostics
- Log selected server extension implementations and override registration guidance at INFO.
- Log a bracketed WARN list when configured UDP uses the default datagram policy or client commands lack a command policy; keep warnings visible when INFO is disabled.

### Network engine shutdown
- Interrupt blocked engine workers before waiting for termination, avoiding the ten-second idle writer shutdown delay.
- Suppress expected TCP/UDP selector-closure errors only after shutdown begins; keep reporting unexpected selector closures and unrelated failures.
- Add shutdown regressions for blocked writers, reader close races, interrupt preservation and repeated shutdown.

### Response API cleanup
- Remove the blocking `Response.writeInDelay(long)` API and its implementation. Applications needing delayed sends should schedule application jobs and call `write()` when due. Existing callers must be updated and recompiled.
- Remove the response implementation's now-unused sleep import and logger inheritance.
