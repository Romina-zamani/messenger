package dev.mahdi.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest {
  @Value("${local.server.port}") int port;
  @Autowired JdbcTemplate database;
  @BeforeEach void clearMessages() { database.update("DELETE FROM messages"); }

  @Test void broadcastsPersistsAndRestoresHistory() throws Exception {
    try (SocketPeer alice = connect(); SocketPeer bob = connect()) {
      assertTrue(alice.take().contains("\"type\":\"READY\""));
      assertTrue(bob.take().contains("\"type\":\"READY\""));

      alice.send("{\"nickname\":\"Impostor\",\"content\":\"Hello WebSocket\"}");
      String aliceEvent = alice.take();
      String bobEvent = bob.take();
      assertTrue(aliceEvent.contains("\"sender\":\"socket_user\""));
      assertFalse(aliceEvent.contains("Impostor"));
      assertNotNull(database.queryForObject("SELECT sender_id FROM messages WHERE content = 'Hello WebSocket'", Long.class));
      assertTrue(bobEvent.contains("\"content\":\"Hello WebSocket\""));
    }

    try (SocketPeer reconnect = connect()) {
      assertTrue(reconnect.take().contains("\"content\":\"Hello WebSocket\""));
      assertTrue(reconnect.take().contains("\"type\":\"READY\""));
    }
  }

  @Test void sendsStructuredErrorForInvalidMessage() throws Exception {
    try (SocketPeer peer = connect()) {
      peer.take();
      peer.send("{\"content\":\"\"}");
      String event = peer.take();
      assertTrue(event.contains("\"type\":\"ERROR\""));
      assertTrue(event.contains("Message must contain"));
    }
  }

  @Test void signalSocketStartsWithReadyAndFeedStatusEvents() throws Exception {
    try (SocketPeer peer = connect("/ws/signals")) {
      assertTrue(peer.take().contains("\"type\":\"READY\""));
      String status = peer.take();
      assertTrue(status.contains("\"type\":\"FEED_STATUS\""));
      assertTrue(status.contains("\"status\":\"DEGRADED\""));
    }
  }

  @Test void rejectsAnonymousChatAndUntrustedOrigin() throws Exception {
    assertThrows(CompletionException.class, () -> new SocketPeer(URI.create("ws://localhost:" + port + "/ws/chat")));
    var client = authenticatedClient();
    assertThrows(CompletionException.class, () -> client.http.newWebSocketBuilder()
        .header("Origin", "https://untrusted.example")
        .buildAsync(URI.create("ws://localhost:" + port + "/ws/chat"), new WebSocket.Listener() {}).join());
  }

  @Test void logoutClosesExistingSocket() throws Exception {
    var client = authenticatedClient();
    try (SocketPeer peer = new SocketPeer(URI.create("ws://localhost:" + port + "/ws/chat"), client.http)) {
      peer.take();
      assertEquals(204, client.post("/api/auth/logout", "", false, true).statusCode());
      assertEquals(1008, peer.closed.get(3, TimeUnit.SECONDS));
      assertThrows(CompletionException.class, () -> new SocketPeer(URI.create("ws://localhost:" + port + "/ws/chat"), client.http));
    }
  }

  private AuthTestClient authenticatedClient() throws Exception {
    var client = new AuthTestClient(port);
    int status = client.register("socket_user").statusCode();
    assertTrue(status == 201 || status == 409);
    assertEquals(204, client.login("socket_user", "LearningJava123!").statusCode());
    return client;
  }
  private SocketPeer connect() throws Exception {
    return new SocketPeer(URI.create("ws://localhost:" + port + "/ws/chat"), authenticatedClient().http);
  }
  private SocketPeer connect(String path) { return new SocketPeer(URI.create("ws://localhost:" + port + path)); }

  private static final class SocketPeer implements WebSocket.Listener, AutoCloseable {
    private final BlockingQueue<String> events = new LinkedBlockingQueue<>();
    private final WebSocket socket;
    private final CompletableFuture<Integer> closed = new CompletableFuture<>();
    SocketPeer(URI uri) {
      this(uri, HttpClient.newHttpClient());
    }
    SocketPeer(URI uri, HttpClient client) {
      socket = client.newWebSocketBuilder()
          .connectTimeout(Duration.ofSeconds(3)).buildAsync(uri, this).join();
    }
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      events.add(data.toString()); webSocket.request(1); return null;
    }
    public void onOpen(WebSocket webSocket) { webSocket.request(1); }
    public CompletionStage<?> onClose(WebSocket webSocket, int status, String reason) {
      closed.complete(status); return null;
    }
    void send(String json) { socket.sendText(json, true).join(); }
    String take() throws InterruptedException {
      String event = events.poll(3, TimeUnit.SECONDS);
      assertNotNull(event, "Expected a WebSocket event within 3 seconds");
      return event;
    }
    public void close() { if (!socket.isOutputClosed()) socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").join(); }
  }
}
