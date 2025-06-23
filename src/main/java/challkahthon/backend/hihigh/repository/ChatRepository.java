package challkahthon.backend.hihigh.repository;

import challkahthon.backend.hihigh.domain.entity.Chat;
import challkahthon.backend.hihigh.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    // Find all chats ordered by updated time (most recent first)
    List<Chat> findAllByOrderByUpdatedAtDesc();

    // Find all chats by user ordered by updated time (most recent first)
    List<Chat> findByUserOrderByUpdatedAtDesc(User user);

    // Find chat by user (should be only one per user)
    Chat findByUser(User user);
}
