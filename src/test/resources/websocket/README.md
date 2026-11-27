# WebSocket test certificate

`localhost-test.jks` contains a self-signed localhost key for integration tests only.
Its password is `changeit`. It is valid through November 2036 and must never be used
for a deployed server. Tests install it only in their own TLS trust context.
