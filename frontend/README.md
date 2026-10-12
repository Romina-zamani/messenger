# Lesson 6 — React WebSocket UI

The chat uses account login and a session cookie. Vite proxies `/api` and `/ws` to the Java backend at `http://localhost:8080`, so authentication and chat share the browser's origin. The existing Live Signals demonstration remains public.

## What happens when you send

```text
React browser
  -- { content } ----------> Java WebSocket handler (authenticated session)
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

Open `http://localhost:5173`, choose **Create an account**, then register. Usernames use 3–30 letters, numbers, or underscores and are stored lowercase. Passwords require at least 12 characters and at most 72 UTF-8 bytes. Registration signs you in automatically.

To test two different users, use separate browser profiles or a private window: tabs in one profile share the same session cookie. Refreshing restores your account; **Log out** invalidates the session and closes its chat sockets. Idle HTTP sessions expire after 30 minutes; WebSocket traffic alone does not renew them.

## Important files

- `src/App.jsx` contains the visible React components and local form state.
- `src/AuthScreen.jsx` contains the registration and login forms.
- `src/authApi.js` obtains CSRF tokens and sends authentication requests.
- `src/useChatSocket.js` owns the WebSocket connection, incoming events, sending, and reconnect behavior.
- `src/useSignals.js` loads the PostgreSQL snapshot and applies Redis-backed WebSocket deltas.
- `src/LiveSignalsPage.jsx` renders the searchable telemetry summary and table.
- `src/styles.css` contains the responsive and accessible visual design.
- `src/App.test.jsx` proves the browser/server message behavior with a fake WebSocket.

## Use secure WebSocket

For the backend TLS lesson, start Java with the TLS profile described in `../docs/lesson-05-tls.md`. Before starting Vite, set its target and trusted public certificate in PowerShell:

```powershell
$env:BACKEND_URL='https://localhost:8443'
# NODE_EXTRA_CA_CERTS must point to a PEM certificate exported from your local certificate.
$env:NODE_EXTRA_CA_CERTS='C:\path\to\localhost.pem'
npm.cmd run dev
```

Keep chat on its default same-origin URL; remove old `VITE_WS_URL` overrides. This encrypts the Vite-to-backend connection, not the browser-to-Vite HTTP connection. For deployment, serve the frontend over HTTPS with `/api` and `/ws` forwarded to Java and secure session cookies. Do not disable certificate validation.

## Exercise

Add a small counter that displays how many server-confirmed messages are currently in the message list. Do not count a message when the send button is clicked; count it only after a `MESSAGE` event arrives.
