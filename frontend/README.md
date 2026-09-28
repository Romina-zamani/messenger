# Lesson 6 — React WebSocket UI

This browser client connects to the Java endpoint at `ws://localhost:8080/ws/chat`.

## What happens when you send

```text
React browser
  -- { nickname, content } --> Java WebSocket handler
  <-- MESSAGE event -------- Java saves and broadcasts
```

The UI does **not** immediately add your text to the message list. It waits for Java to validate, save, timestamp, and broadcast the accepted message. This is called a server-authoritative flow.

## Run it

First start PostgreSQL and the Java backend from the project root:

```powershell
docker compose up -d
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
mvn.cmd -pl backend spring-boot:run
```

In a second VS Code terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`. Open it in two tabs, use different nicknames, and send messages.

## Important files

- `src/App.jsx` contains the visible React components and local form state.
- `src/useChatSocket.js` owns the WebSocket connection, incoming events, sending, and reconnect behavior.
- `src/styles.css` contains the responsive and accessible visual design.
- `src/App.test.jsx` proves the browser/server message behavior with a fake WebSocket.

## Use secure WebSocket

Start the backend with the TLS profile described in `../docs/lesson-05-tls.md`, then create `frontend/.env.local`:

```text
VITE_WS_URL=wss://localhost:8443/ws/chat
```

Restart Vite after changing an environment file. The browser must trust the local certificate before `wss://` can connect.

## Exercise

Add a small counter that displays how many server-confirmed messages are currently in the message list. Do not count a message when the send button is clicked; count it only after a `MESSAGE` event arrives.
