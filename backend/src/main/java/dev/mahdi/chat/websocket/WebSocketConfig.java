package dev.mahdi.chat.websocket;
import dev.mahdi.chat.signal.SignalSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final ChatWebSocketHandler handler;
  private final SignalSocketHandler signals;
  public WebSocketConfig(ChatWebSocketHandler handler, SignalSocketHandler signals){this.handler=handler;this.signals=signals;}
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
    registry.addHandler(handler,"/ws/chat").setAllowedOriginPatterns("http://localhost:*","https://localhost:*");
    registry.addHandler(signals,"/ws/signals").setAllowedOriginPatterns("http://localhost:*","https://localhost:*");
  }
}

