package challkahthon.backend.hihigh.domain.dto.response;

import challkahthon.backend.hihigh.domain.entity.Chat;
import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDto {
    private Long id;
    private String title;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatMessageDto> messages;

    public static ChatDto fromEntity(Chat chat) {
        return ChatDto.builder()
                .id(chat.getId())
                .username(chat.getUser() != null ? chat.getUser().getLoginId() : null)
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
    }

    public static ChatDto fromEntityWithMessages(Chat chat, List<ChatMessage> messages) {
        ChatDto chatDto = fromEntity(chat);
        chatDto.setMessages(messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList()));
        return chatDto;
    }
}
