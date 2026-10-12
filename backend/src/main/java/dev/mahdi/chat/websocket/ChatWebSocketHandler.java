package dev.mahdi.chat.websocket;
import tools.jackson.databind.ObjectMapper;
import dev.mahdi.chat.message.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
  private final MessageService messages; private final ObjectMapper json; private final Set<WebSocketSession> sessions=ConcurrentHashMap.newKeySet();
  private final ChatSessions authenticatedSessions;
  public ChatWebSocketHandler(MessageService messages,ObjectMapper json,ChatSessions authenticatedSessions){this.messages=messages;this.json=json;this.authenticatedSessions=authenticatedSessions;}
  @Override public void afterConnectionEstablished(WebSocketSession session)throws Exception{
    if (!authenticatedSessions.attach(session)) { ChatSessions.close(session); return; }
    sessions.add(session);
    for(ChatMessage message:messages.recent()) send(session,ServerEvent.message(message));
    send(session,new ServerEvent("READY",null,null,null,Instant.now(),"Connected",null));
  }
  @Override protected void handleTextMessage(WebSocketSession session,TextMessage text)throws Exception{
    if (!authenticatedSessions.valid(session)) { ChatSessions.close(session); return; }
    try { IncomingMessage incoming=json.readValue(text.getPayload(),IncomingMessage.class); broadcast(ServerEvent.message(messages.createForUser(session.getPrincipal().getName(),incoming.content()))); }
    catch(IllegalArgumentException error){send(session,ServerEvent.error(error.getMessage()));}
    catch(Exception error){send(session,ServerEvent.error("Invalid JSON message."));}
  }
  @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){sessions.remove(session);authenticatedSessions.detach(session);}
  private void broadcast(ServerEvent event){sessions.removeIf(session->{try{send(session,event);return false;}catch(IOException e){return true;}});}
  private void send(WebSocketSession session,ServerEvent event)throws IOException{synchronized(session){if(!authenticatedSessions.valid(session)){ChatSessions.close(session);return;}if(session.isOpen())session.sendMessage(new TextMessage(json.writeValueAsString(event)));}}
  public record IncomingMessage(String nickname,String content){}
  public record ServerEvent(String type,Long id,String sender,String content,Instant sentAt,String detail,Long senderId){
    static ServerEvent message(ChatMessage m){return new ServerEvent("MESSAGE",m.id(),m.nickname(),m.content(),m.sentAt(),null,m.senderId());}
    static ServerEvent error(String detail){return new ServerEvent("ERROR",null,null,null,Instant.now(),detail,null);}
  }
}
