package challkahthon.backend.hihigh.repository;

import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Find all messages for a chat ordered by timestamp
    List<ChatMessage> findByChatIdOrderByTimestamp(Long chatId);
}