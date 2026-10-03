# Java Messenger Learning Project

Build a messenger one concept at a time:

1. [`lesson-01-one-client`](lesson-01-one-client/README.md): one blocking TCP connection and request/reply.
2. [`lesson-02-multi-client`](lesson-02-multi-client/README.md): threads, connected clients, and broadcast.
3. [`backend`](backend/README.md): Spring Boot, PostgreSQL, Flyway persistence, health, and WebSocket.
4. [`frontend`](frontend/README.md): React browser client with connection state, reconnect, and server-authoritative messages.
5. [`docs/lesson-05-tls.md`](docs/lesson-05-tls.md): HTTPS certificates and secure WebSocket (`wss://`).
6. Live Signals: PostgreSQL-backed telemetry distributed through Redis Pub/Sub and `/ws/signals` deltas.

Source belongs under `src/main`; tests belong under `src/test`; generated binary `.class` files belong under `target` and should not be edited.
