package dev.mahdi.chat.message;
import java.time.Instant;
public record ChatMessage(Long id, String nickname, String content, Instant sentAt) {}

