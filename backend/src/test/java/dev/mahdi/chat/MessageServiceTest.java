package dev.mahdi.chat;

import dev.mahdi.chat.message.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MessageServiceTest {
  @Autowired MessageService service;

  @Test void savesValidMessageAndReturnsItInHistory() {
    ChatMessage saved = service.create(" Alice ", " Hello WebSocket ");
    assertEquals("Alice", saved.nickname());
    assertEquals("Hello WebSocket", saved.content());
    assertNotNull(saved.sentAt());
    assertTrue(service.recent().stream().anyMatch(message -> message.id().equals(saved.id())));
  }

  @Test void rejectsBlankNicknameAndOversizedContent() {
    assertThrows(IllegalArgumentException.class, () -> service.create(" ", "hello"));
    assertThrows(IllegalArgumentException.class, () -> service.create("Alice", "x".repeat(501)));
  }
}

