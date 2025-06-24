package challkahthon.backend.hihigh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "토큰 유효성 검증 응답 DTO")
public class TokenValidationResponseDto {
    
    @Schema(description = "토큰 유효성", example = "true")
    private boolean valid;
    
    @Schema(description = "사용자 이름", example = "홍길동")
    private String username;
    
    @Schema(description = "토큰 만료 시간", example = "2024-12-26T10:30:00")
    private LocalDateTime expiresAt;
    
    @Schema(description = "토큰 발급 시간", example = "2024-12-25T10:30:00")
    private LocalDateTime issuedAt;
    
    @Schema(description = "토큰 타입", example = "ACCESS")
    private String tokenType;
    
    @Schema(description = "메시지", example = "토큰이 유효합니다.")
    private String message;
    
    /**
     * 유효한 토큰에 대한 응답 생성
     */
    public static TokenValidationResponseDto valid(String username, LocalDateTime issuedAt, LocalDateTime expiresAt, String tokenType) {
        return TokenValidationResponseDto.builder()
                .valid(true)
                .username(username)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .tokenType(tokenType)
                .message("토큰이 유효합니다.")
                .build();
    }
    
    /**
     * 유효하지 않은 토큰에 대한 응답 생성
     */
    public static TokenValidationResponseDto invalid(String message) {
        return TokenValidationResponseDto.builder()
                .valid(false)
                .message(message)
                .build();
    }
}
