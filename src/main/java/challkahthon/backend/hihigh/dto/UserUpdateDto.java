package challkahthon.backend.hihigh.dto;

import challkahthon.backend.hihigh.domain.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "사용자 정보 업데이트 DTO")
public class UserUpdateDto {
    
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
    
    @Schema(description = "성별", example = "MALE")
    private Gender gender;
    
    @Schema(description = "출생년도", example = "1995")
    private String birthYear;
    
    @Schema(description = "사용자 관심사", example = "프로그래밍, 디자인, 마케팅")
    private String interests;
    
    @Schema(description = "사용자 목표", example = "백엔드 개발자 되기, 창업하기")
    private String goals;
    
    @Schema(description = "사용자 희망직종", example = "소프트웨어 엔지니어, 데이터 사이언티스트")
    private String desiredOccupation;
}