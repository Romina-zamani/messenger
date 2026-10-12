package dev.mahdi.chat.websocket;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WebSocketConsoleClientTest {
  @Test void createsValidJsonWhenTextContainsQuotes() {
    String json = WebSocketConsoleClient.toJson("Say \"hello\"");
    assertFalse(json.contains("nickname"));
    assertTrue(json.contains("Say \\\"hello\\\""));
  }
}
