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
