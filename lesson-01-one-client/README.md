# Lesson 1: one client talks to one server

This lesson uses raw TCP. It is intentionally not WebSocket yet: first you see how a server opens a port, waits, accepts one connection, reads bytes, and writes a reply.

## What happens

```text
Server                                      Client
new ServerSocket(5000)
accept() -> waits
                                  new Socket("localhost", 5000)
accept() returns <---------------- TCP connection
readLine() -> waits  <------------ "hello\n"
println()             ------------> "Echo: hello\n"
```

`localhost` means this computer. A port identifies one network program on that computer. `accept()` and `readLine()` are blocking operations: the current thread sleeps until useful work arrives.

## Build and run

Use JDK 21 in each new PowerShell window:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
mvn -f lesson-01-one-client/pom.xml test
```

Terminal 1, start the server:

```powershell
mvn -f lesson-01-one-client/pom.xml compile exec:java "-Dexec.mainClass=dev.mahdi.chat.lesson1.EchoServer"
```

Terminal 2, start the client:

```powershell
mvn -f lesson-01-one-client/pom.xml compile exec:java "-Dexec.mainClass=dev.mahdi.chat.lesson1.EchoClient"
```

Type several messages, then `quit`. The server handles one client and exits. That limitation is the subject of Lesson 2.

## Experiments

1. Start the client before the server and read the friendly connection error.
2. Start the server and inspect the listening port with `Get-NetTCPConnection -LocalPort 5000`.
3. Start a second client while the first is connected. Notice that Lesson 1 cannot serve it concurrently.

## Common errors

- **Connection refused:** no server is listening at that host and port.
- **Address already in use:** another process already owns port 5000.
- **Client appears frozen:** it may be waiting in `readLine()` for the server's newline.

## Exercise

Change the server response from `Echo:` to `Received:`. Update the test first, watch it fail, then change the server.
