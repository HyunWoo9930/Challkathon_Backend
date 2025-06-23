package challkahthon.backend.hihigh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "사용자 관심사 업데이트 DTO")
public class InterestsUpdateDto {
    
    @Schema(description = "사용자 관심사", example = "프로그래밍, 디자인, 마케팅")
    private String interests;
}