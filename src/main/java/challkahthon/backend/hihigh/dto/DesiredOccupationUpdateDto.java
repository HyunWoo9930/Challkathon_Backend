package challkahthon.backend.hihigh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "사용자 희망직종 업데이트 DTO")
public class DesiredOccupationUpdateDto {
    
    @Schema(description = "사용자 희망직종", example = "소프트웨어 엔지니어, 데이터 사이언티스트")
    private String desiredOccupation;
}