package challkahthon.backend.hihigh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "사용자 목표 업데이트 DTO")
public class GoalsUpdateDto {
    
    @Schema(description = "사용자 목표", example = "백엔드 개발자 되기, 창업하기")
    private String goals;
}