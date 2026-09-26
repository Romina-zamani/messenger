package dev.mahdi.chat.message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface MessageRepository extends JpaRepository<MessageEntity,Long> {
  List<MessageEntity> findTop50ByOrderBySentAtDesc();
}

