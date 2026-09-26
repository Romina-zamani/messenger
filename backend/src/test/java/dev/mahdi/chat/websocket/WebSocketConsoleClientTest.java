package dev.mahdi.chat.websocket;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WebSocketConsoleClientTest {
  @Test void createsValidJsonWhenTextContainsQuotes() {
    String json = WebSocketConsoleClient.toJson("Alice", "Say \"hello\"");
    assertTrue(json.contains("\"nickname\":\"Alice\""));
    assertTrue(json.contains("Say \\\"hello\\\""));
  }
}
