# Lessons 3 and 4: PostgreSQL, Spring Boot, and WebSocket

## Lesson 3: persistent messages

The console servers stored clients only in memory. This backend adds PostgreSQL:

```text
MessageService.create()
        |
        v
MessageRepository (Spring Data JPA)
        |
        v
PostgreSQL messages table
```

`MessageEntity` maps Java fields to database columns. `MessageRepository` supplies SQL operations. `MessageService` validates input and owns the transaction. Flyway runs `V1__create_messages.sql` once and records it in `flyway_schema_history`.

### Start PostgreSQL and Redis

From the project root:

```powershell
docker compose up -d postgres redis
docker compose ps
```

The project exposes PostgreSQL at `localhost:5433` because this computer already has another PostgreSQL server using port 5432. Inside Docker, PostgreSQL still uses its normal port 5432.

### Start the Java backend

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
mvn -f backend/pom.xml spring-boot:run
```

Check health in another terminal:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

### Inspect the database

List tables:

```powershell
docker compose exec postgres psql -U messenger -d messenger -c "\dt"
```

Read messages:

```powershell
docker compose exec postgres psql -U messenger -d messenger -c "SELECT * FROM messages ORDER BY sent_at;"
```

You can insert a learning row before the WebSocket client exists:

```powershell
docker compose exec postgres psql -U messenger -d messenger -c "INSERT INTO messages(nickname,content,sent_at) VALUES ('Student','PostgreSQL remembers me',CURRENT_TIMESTAMP);"
```

Stop and restart the backend with `Ctrl+C` and `mvn ... spring-boot:run`. Query again: the row remains because it is in PostgreSQL, not Java memory. Even `docker compose down` keeps the named volume; `docker compose down -v` deletes it, so do not use `-v` unless you intend to erase lesson data.

### Tests and exercise

Run `mvn -f backend/pom.xml test`. Tests use a temporary H2 database in PostgreSQL compatibility mode, so they are fast and do not change your learning database.

Exercise: add the next unused Flyway version that adds an `edited BOOLEAN NOT NULL DEFAULT FALSE` column. V1–V4 are already assigned. Never edit a migration after it has run.

## Live Signals

The backend seeds 1,000 server telemetry records in PostgreSQL. Every 100 ms the simulator updates one row, then publishes only that committed row's delta to the Redis `messenger:signals` channel. The browser loads its initial state from `GET /api/signals` and receives subsequent updates from `ws://localhost:8080/ws/signals`.

PostgreSQL is authoritative. If Redis is unavailable, database updates continue and connected clients see a degraded feed state. A reconnect reloads the PostgreSQL snapshot before reopening the WebSocket.

Useful configuration variables are `REDIS_HOST`, `REDIS_PORT`, `SIGNAL_SIMULATOR_ENABLED`, and `SIGNAL_SIMULATOR_INTERVAL_MS`.

## Lesson 4 preview: WebSocket

The endpoint is `ws://localhost:8080/ws/chat`. It requires an authenticated session and accepts JSON:

```json
{"content":"Hello"}
```

The server obtains the sender from the authenticated session, validates and saves the message, then broadcasts a server-owned event. Supplying a different nickname cannot change the sender. V4 links new messages to user IDs; existing messages keep their original nicknames and have no user ID.

## Account login: Java learning guide

1. `user/AccountService.java` validates registration and calls `PasswordEncoder.encode`. BCrypt stores a salted hash, not the original password. It writes the account through `UserRepository`.
2. `user/AuthController.java` exposes registration and the current account. Its response contains only ID and username, never the password hash.
3. `user/SecurityConfig.java` defines the security rules. Spring Security handles the login POST, checks the password, rotates the session ID, and keeps the authenticated user in an HTTP session. Logout invalidates that session.
4. `websocket/ChatSessions.java` connects that HTTP session to live sockets and closes them when the session ends. `ChatWebSocketHandler` uses the authenticated principal (the server's representation of the user) as the sender.
5. `MessageService.createForUser` stores both the account ID and its username. Existing nickname-only messages are not assigned to new accounts.

Authentication endpoints:

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/auth/csrf` | Obtain a token and header name for the next POST |
| POST | `/api/auth/register` | JSON `{ "username": "romina", "password": "your chosen password" }`; creates an account (201), but does not log in |
| POST | `/api/auth/login` | Form-encoded `username` and `password`; starts a session (204) |
| GET | `/api/auth/me` | Returns account ID/username, or 401 when signed out |
| POST | `/api/auth/logout` | Ends the session (204) |

All POSTs require the CSRF token header and session cookie. Obtain a fresh token after login/logout because security rotates it. The React client does this automatically. Account names are trimmed and lowercased; duplicate names return 409. Invalid registration returns 400, bad credentials return 401, and missing CSRF returns 403.

The session lives in this Java process and is lost on restart; account data remains in PostgreSQL. The room is still shared by all signed-in users. Email recovery, login rate limiting, and private conversations are not included in this learning step. Use HTTPS and `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` when hosting beyond localhost. The existing signals demo remains public.

### Test WebSocket in terminals

Keep PostgreSQL and the backend running. In two new terminals run:

```powershell
mvn -f backend/pom.xml compile exec:java "-Dexec.mainClass=dev.mahdi.chat.websocket.WebSocketConsoleClient"
```

First register accounts in the browser. The terminal client now asks for a registered username and password before opening its WebSocket. When launched through Maven, password input may be visible; the prompt warns you. A message sent in either terminal appears in both as a JSON `MESSAGE` event. Stop and reconnect a client to see stored history arrive before the `READY` event.

Unlike Lesson 2's plain TCP protocol, WebSocket begins as HTTP. The client requests an upgrade, the server replies `101 Switching Protocols`, and then both sides exchange framed messages over the same connection.
