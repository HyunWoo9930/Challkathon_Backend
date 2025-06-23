package challkahthon.backend.hihigh.domain.dto.response;

import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime timestamp;
    
    public static ChatMessageDto fromEntity(ChatMessage chatMessage) {
        return ChatMessageDto.builder()
                .id(chatMessage.getId())
                .role(chatMessage.getRole())
                .content(chatMessage.getContent())
                .timestamp(chatMessage.getTimestamp())
                .build();
    }
}