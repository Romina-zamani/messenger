# Lesson 2: many clients

Lesson 1 blocked inside one connection. Here the accept loop keeps accepting, while an `ExecutorService` runs one `ClientHandler` per connection. `ConcurrentHashMap.newKeySet()` lets handlers safely add/remove themselves while other threads broadcast.

```text
accept loop -> ClientHandler(Alice) --\
             ClientHandler(Bob)   ----> thread-safe clients -> broadcast
             ClientHandler(Sara)  --/
```

Run the test with `mvn -f lesson-02-multi-client/pom.xml test`. Start `ChatServer` in one terminal, then run `ChatClient` in three other terminals using the same `exec:java` pattern from Lesson 1.

Try closing one client window without typing `quit`; `finally` still removes it. Exercise: reject a nickname already connected, starting with a failing test.
