package dev.mahdi.chat.signal;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalSocketHandler extends TextWebSocketHandler {
  private final ObjectMapper json;
  private final SignalFeedState feed;
  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
  SignalSocketHandler(ObjectMapper json, SignalFeedState feed) { this.json = json; this.feed = feed; }

  @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessions.add(session);
    send(session, new ControlEvent("READY", null, Instant.now(), "Signal subscription active"));
    send(session, new ControlEvent("FEED_STATUS", feed.get(), Instant.now(), null));
  }
  @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { sessions.remove(session); }
  public void broadcastUpdate(SignalUpdate update) { broadcast(update); }
  public void broadcastStatus() { broadcast(new ControlEvent("FEED_STATUS", feed.get(), Instant.now(), null)); }
  private void broadcast(Object event) { sessions.removeIf(session -> { try { send(session, event); return false; } catch (IOException error) { return true; } }); }
  private void send(WebSocketSession session, Object event) throws IOException { synchronized (session) { if (session.isOpen()) session.sendMessage(new TextMessage(json.writeValueAsString(event))); } }
  record ControlEvent(String type, String status, Instant at, String detail) {}
}
