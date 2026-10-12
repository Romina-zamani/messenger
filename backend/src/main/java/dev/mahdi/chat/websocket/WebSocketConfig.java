package dev.mahdi.chat.websocket;
import dev.mahdi.chat.signal.SignalSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final ChatWebSocketHandler handler;
  private final SignalSocketHandler signals;
  private final ChatSessions sessions;
  public WebSocketConfig(ChatWebSocketHandler handler, SignalSocketHandler signals, ChatSessions sessions){this.handler=handler;this.signals=signals;this.sessions=sessions;}
  @Bean ServletListenerRegistrationBean<ChatSessions> chatSessionListener() {
    return new ServletListenerRegistrationBean<>(sessions);
  }
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
    registry.addHandler(handler,"/ws/chat").addInterceptors(sessions).setAllowedOrigins("http://localhost:5173","http://127.0.0.1:5173","https://localhost:5173","https://127.0.0.1:5173");
    registry.addHandler(signals,"/ws/signals").setAllowedOriginPatterns("http://localhost:*","https://localhost:*","http://127.0.0.1:*","https://127.0.0.1:*");
  }
}

