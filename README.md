<p align="center">
    <a href="#">
        <img src="https://raw.githubusercontent.com/congcoi123/tenio/refs/heads/master/assets/tenio-github-logo.png">
    </a>
</p>
<p align="center">
    <a href="https://mvnrepository.com/artifact/io.github.congcoi123/tenio-core">
        <img src="https://img.shields.io/maven-central/v/io.github.congcoi123/tenio-core.svg">
    </a>
    <a href="https://javadoc.io/doc/io.github.congcoi123/tenio-core">
        <img src="https://javadoc.io/badge2/io.github.congcoi123/tenio-core/javadoc.svg">
    </a>
    <a href="LICENSE">
        <img src="https://img.shields.io/badge/license-MIT-blue.svg">
    </a>
    <a href="https://github.com/congcoi123/tenio-core/actions">
        <img src="https://github.com/congcoi123/tenio-core/actions/workflows/maven.yml/badge.svg">
    </a>    
    <a href="https://coveralls.io/github/congcoi123/tenio-core">
        <img src="https://coveralls.io/repos/github/congcoi123/tenio-core/badge.svg?branch=master">
    </a>
    <a href="#">
        <img src="https://img.shields.io/github/last-commit/congcoi123/tenio-core">
    </a>
    <a href="https://github.com/congcoi123/tenio-core/issues">
        <img src="https://img.shields.io/github/issues/congcoi123/tenio-core">
    </a>
    <a href="CONTRIBUTING.md">
        <img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg">
    </a>
    <a href="https://discord.gg/ybkNU87Psy">
        <img src="https://img.shields.io/discord/1146091189456613407?logo=discord&logoColor=white">
    </a>
</p>

# TenIO Core Module [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=TenIO%20is%20a%20java%20NIO%20based%20server%20specifically%20designed%20for%20multiplayer%20games.%0D%0A&url=https://github.com/congcoi123/tenio%0D%0A&hashtags=tenio,java,gameserver,multiplayer,nio,netty,jetty,msgpack,cocos2dx,unity,libgdx,phaserjs%0D%0A&via=congcoi123)
[`TenIO`](https://github.com/congcoi123/tenio) is an open-source project to create multiplayer online games that includes a java NIO (Non-blocking I/O) 
based server specifically designed for multiplayer games, which supports UDP, TCP, Websocket, HTTP transports, and available simple client projects for quick development.

This module provides all the main features for the framework to operate. It contains an NIO mechanism, server configuration solution, events handling, and other necessary functions that you may find helpful.

## All supported events
```txt
EventAccessDatagramChannelRequestValidation
EventAccessDatagramChannelRequestValidationResult
EventBroadcastToChannel
EventChannelCreated
EventChannelWillBeRemoved
EventConnectionEstablishedResult
EventConnectionWillBeClosed
EventDisconnectPlayer
EventFetchedBandwidthInfo
EventFetchedCcuInfo
EventPlayerAfterLeftRoom
EventPlayerBeforeLeaveRoom
EventPlayerConnectionReplacing
EventPlayerJoinedRoomResult
EventPlayerLogin
EventPlayerConnectionResumed
EventPlayerConnectionRetry
EventPlayerSubscribedChannel
EventPlayerUnsubscribedChannel
EventReceivedMessageFromPlayer
EventRoomCreatedResult
EventRoomWillBeRemoved
EventSendMessageToPlayer
EventServerInitialization
EventServerTeardown
EventSocketConnectionRefused
EventSwitchParticipantToSpectatorResult
EventSwitchSpectatorToParticipantResult
EventSystemMonitoring
EventWebSocketConnectionRefused
EventWriteMessageToConnection
```

## Customizable behaviors
```txt
Just create your own classes, implement desired interfaces and annotate them with @Component.
In case you do not declare any change, their default implementations will be applied
(DefaultConnectionFilter, DefaultOutboundQueuePolicy, DefaultDatagramPacketPolicy).

packet-encryptor: BinaryPacketEncryptor
packet-compressor: BinaryPacketCompressor
connection-filter: ConnectionFilter
outbound-queue-policy: OutboundQueuePolicy
request-policy: RequestPolicy
datagram-packet-policy: DatagramPacketPolicy
```

## Requirements
Java 21+

## Dependencies
- HTTP [Jetty](https://eclipse.dev/jetty)
- Websocket [Netty](https://netty.io)

## License
The [`TenIO`](https://github.com/congcoi123/tenio) project is currently available under the [MIT](LICENSE) License.

## Changelog
Please check out the [changelog](CHANGELOG.md) for more details.

## Contributing
Please check out the [checklist](CHECKLIST.md) and [contributing guideline](CONTRIBUTING.md) for more details.

## Installation
Maven Central
```xml
<dependency>
    <groupId>io.github.congcoi123</groupId>
    <artifactId>tenio-core</artifactId>
    <version>1.0.0</version>
</dependency>
```
GitHub
```sh
$ git clone https://github.com/congcoi123/tenio-core.git
```

> Happy coding !

## Desktop administration

With `enable-terminal-command` set to `true`, TenIO opens a Swing window using the system look and feel.
Closing the window leaves the server running. A headless JVM skips the window. The embedded terminal
input loop is removed. Local registered commands such as `help` send their results to the window via
`CommandUtility.showConsoleMessage`. Direct writes to stdout still go to the server log/console.

The Statistics tab refreshes every five seconds while visible; the Players tab plots the last 60 samples.
Use `stats`, `session <id>`, `room <id>`, `network pressure`, `limits`, or `history [sessionId]`.
Pressure is measured at 80 percent of configured outbound capacity. The pressure view scans active
sessions on demand and returns up to 50 matches. Routine stats use aggregate counters. Independent UDP
receive timestamps are currently unavailable. Room queries return at most 50 member identities.

### Command limits and history

Set `requestsPerSecond` and optionally `burst` on a client command. The injector registers the
policy once. Invoke the command through `ClientCommandManager.invoke` after resolving its code.
For application-defined routers configure a rule once with `getLimits().configure(code, rate, burst)`
and call `allow(code, playerIdentity)` before dispatch. For pre-login handlers call
`allowSession(code, sessionId)`. Configure during startup for automatic session cleanup.
A rejection skips the handler. Applications control any reply. Unconfigured codes remain unrestricted.
Each rule caps its identity table at 65,536, expires fully refilled entries on use, and rejects
new identities if the cap is reached. Accepted and rejected totals are grouped by command code.

Enable `history` to retain the latest 1,024 lifecycle and pressure events per instance. Use
`server.getInspection().history().enableExport(backlog, consumer)` during initialization to
persist events in your application. A blocked exporter fills only its bounded backlog; overflow
and callback failures increment the dropped-export counter. The history view returns at most 50 events.
A blank server-level profile property selects unrestricted components. Omitting the property in
an external configuration still fails validation. The history profile is optional.
