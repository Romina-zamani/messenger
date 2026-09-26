package dev.mahdi.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

  @Test void broadcastsPersistsAndRestoresHistory() throws Exception {
    try (SocketPeer alice = connect(); SocketPeer bob = connect()) {
      assertTrue(alice.take().contains("\"type\":\"READY\""));
      assertTrue(bob.take().contains("\"type\":\"READY\""));

      alice.send("{\"nickname\":\"Alice\",\"content\":\"Hello WebSocket\"}");
      String aliceEvent = alice.take();
      String bobEvent = bob.take();
      assertTrue(aliceEvent.contains("\"sender\":\"Alice\""));
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
      peer.send("{\"nickname\":\"\",\"content\":\"hello\"}");
      String event = peer.take();
      assertTrue(event.contains("\"type\":\"ERROR\""));
      assertTrue(event.contains("Nickname must contain"));
    }
  }

  private SocketPeer connect() {
    return new SocketPeer(URI.create("ws://localhost:" + port + "/ws/chat"));
  }

  private static final class SocketPeer implements WebSocket.Listener, AutoCloseable {
    private final BlockingQueue<String> events = new LinkedBlockingQueue<>();
    private final WebSocket socket;
    SocketPeer(URI uri) {
      socket = HttpClient.newHttpClient().newWebSocketBuilder()
          .connectTimeout(Duration.ofSeconds(3)).buildAsync(uri, this).join();
    }
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      events.add(data.toString()); webSocket.request(1); return null;
    }
    public void onOpen(WebSocket webSocket) { webSocket.request(1); }
    void send(String json) { socket.sendText(json, true).join(); }
    String take() throws InterruptedException {
      String event = events.poll(3, TimeUnit.SECONDS);
      assertNotNull(event, "Expected a WebSocket event within 3 seconds");
      return event;
    }
    public void close() { socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").join(); }
  }
}
