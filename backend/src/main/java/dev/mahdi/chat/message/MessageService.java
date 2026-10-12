package dev.mahdi.chat.message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;
import dev.mahdi.chat.user.UserRepository;
@Service
public class MessageService {
  private final MessageRepository repository; private final Clock clock=Clock.systemUTC();
  private final UserRepository users;
  MessageService(MessageRepository repository, UserRepository users){this.repository=repository;this.users=users;}
  @Transactional public ChatMessage createForUser(String username, String content) {
    var user = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Account not found."));
    String cleanContent = content == null ? "" : content.trim();
    if(cleanContent.isBlank() || cleanContent.length() > 500) throw new IllegalArgumentException("Message must contain 1 to 500 characters.");
    var entity = new MessageEntity(user.getUsername(), cleanContent, clock.instant());
    entity.senderId = user.getId();
    return toMessage(repository.save(entity));
  }
  @Transactional public ChatMessage create(String nickname,String content){
    String cleanName=nickname==null?"":nickname.trim(); String cleanContent=content==null?"":content.trim();
    if(cleanName.isBlank()||cleanName.length()>30) throw new IllegalArgumentException("Nickname must contain 1 to 30 characters.");
    if(cleanContent.isBlank()||cleanContent.length()>500) throw new IllegalArgumentException("Message must contain 1 to 500 characters.");
    return toMessage(repository.save(new MessageEntity(cleanName,cleanContent,clock.instant())));
  }
  @Transactional(readOnly=true) public List<ChatMessage> recent(){
    List<ChatMessage> result=new ArrayList<>(repository.findTop50ByOrderBySentAtDesc().stream().map(this::toMessage).toList());
    Collections.reverse(result); return result;
  }
  private ChatMessage toMessage(MessageEntity e){return new ChatMessage(e.id,e.nickname,e.content,e.sentAt,e.senderId);}
}

