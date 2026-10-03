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

Exercise: add a second Flyway file named `V2__add_edited_flag.sql` that adds an `edited BOOLEAN NOT NULL DEFAULT FALSE` column. Never edit V1 after it has run.

## Live Signals

The backend seeds 1,000 server telemetry records in PostgreSQL. Every 100 ms the simulator updates one row, then publishes only that committed row's delta to the Redis `messenger:signals` channel. The browser loads its initial state from `GET /api/signals` and receives subsequent updates from `ws://localhost:8080/ws/signals`.

PostgreSQL is authoritative. If Redis is unavailable, database updates continue and connected clients see a degraded feed state. A reconnect reloads the PostgreSQL snapshot before reopening the WebSocket.

Useful configuration variables are `REDIS_HOST`, `REDIS_PORT`, `SIGNAL_SIMULATOR_ENABLED`, and `SIGNAL_SIMULATOR_INTERVAL_MS`.

## Lesson 4 preview: WebSocket

The endpoint is `ws://localhost:8080/ws/chat`. It accepts JSON:

```json
{"nickname":"Alice","content":"Hello"}
```

The server validates and saves it, then broadcasts a server-owned event. The React client is added in the next lesson.

### Test WebSocket in terminals

Keep PostgreSQL and the backend running. In two new terminals run:

```powershell
mvn -f backend/pom.xml compile exec:java "-Dexec.mainClass=dev.mahdi.chat.websocket.WebSocketConsoleClient"
```

Choose different nicknames. A message sent in either terminal appears in both as a JSON `MESSAGE` event. Stop and reconnect a client to see stored history arrive before the `READY` event.

Unlike Lesson 2's plain TCP protocol, WebSocket begins as HTTP. The client requests an upgrade, the server replies `101 Switching Protocols`, and then both sides exchange framed messages over the same connection.
