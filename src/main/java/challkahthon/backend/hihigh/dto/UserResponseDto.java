package challkahthon.backend.hihigh.dto;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.domain.enums.Gender;
import challkahthon.backend.hihigh.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 정보 응답 DTO")
public class UserResponseDto {

	@Schema(description = "사용자 ID", example = "1")
	private Long id;

	@Schema(description = "로그인 ID", example = "user123@gmail.com")
	private String loginId;

	@Schema(description = "사용자 권한", example = "USER")
	private UserRole userRole;

	@Schema(description = "사용자 이름", example = "홍길동")
	private String name;

	@Schema(description = "성별", example = "MALE")
	private Gender gender;

	@Schema(description = "출생년도", example = "1990")
	private String birthYear;

	@Schema(description = "개인정보 수집 동의 여부", example = "true")
	private Boolean isPrivateInformAgreed;

	@Schema(description = "OAuth 제공자", example = "google")
	private String provider;

	@Schema(description = "OAuth 제공자 ID")
	private String providerId;

	@Schema(description = "사용자 관심사", example = "프로그래밍, 웹 개발, 자바")
	private String interests;

	@Schema(description = "사용자 목표", example = "풀스택 개발자가 되기")
	private String goals;

	@Schema(description = "희망 직종", example = "백엔드 개발자")
	private String desiredOccupation;

	/**
	 * User 엔터티로부터 UserResponseDto 생성
	 * @param user User 엔터티
	 * @return UserResponseDto
	 */
	public static UserResponseDto fromEntity(User user) {
		return UserResponseDto.builder()
			.id(user.getId())
			.loginId(user.getLoginId())
			.userRole(user.getUserRole())
			.name(user.getName())
			.gender(user.getGender())
			.birthYear(user.getBirthYear())
			.isPrivateInformAgreed(user.getIsPrivateInformAgreed())
			.provider(user.getProvider())
			.providerId(user.getProviderId())
			.interests(user.getInterests())
			.goals(user.getGoals())
			.desiredOccupation(user.getDesiredOccupation())
			.build();
	}
}
