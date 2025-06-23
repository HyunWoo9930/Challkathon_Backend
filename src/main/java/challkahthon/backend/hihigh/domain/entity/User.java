package challkahthon.backend.hihigh.domain.entity;

import challkahthon.backend.hihigh.domain.enums.Gender;
import challkahthon.backend.hihigh.domain.enums.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String loginId;
	private UserRole userRole;
	private String password;
	private String name;
	private Gender gender;
	private String birthYear;
	private Boolean isPrivateInformAgreed;
	// provider : google이 들어감
	private String provider;
	// providerId : 구굴 로그인 한 유저의 고유 ID가 들어감
	private String providerId;
	// 사용자 관심사
	private String interests;
	// 사용자 목표
	private String goals;
	// 사용자 희망직종
	private String desiredOccupation;

	@Builder
	public User(String loginId, String password, UserRole userRole, String name, Gender gender, String birthYear,
		Boolean isPrivateInformAgreed, String provider, String providerId, String interests, String goals, 
		String desiredOccupation) {
		this.loginId = loginId;
		this.userRole = userRole;
		this.name = name;
		this.password = password;
		this.gender = gender;
		this.birthYear = birthYear;
		this.isPrivateInformAgreed = isPrivateInformAgreed;
		this.provider = provider;
		this.providerId = providerId;
		this.interests = interests;
		this.goals = goals;
		this.desiredOccupation = desiredOccupation;
	}
}
