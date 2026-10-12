package dev.mahdi.chat.message;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="messages")
class MessageEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
  @Column(nullable=false,length=30) String nickname;
  @Column(nullable=false,length=500) String content;
  @Column(name="sent_at",nullable=false) Instant sentAt;
  @Column(name="sender_id") Long senderId;
  protected MessageEntity() {}
  MessageEntity(String nickname,String content,Instant sentAt){this.nickname=nickname;this.content=content;this.sentAt=sentAt;}
}

