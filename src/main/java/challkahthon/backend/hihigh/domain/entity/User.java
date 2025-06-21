package challkahthon.backend.hihigh.domain.entity;

import challkahthon.backend.hihigh.domain.enums.Gender;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class User {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String userId;
	private String password;
	private String name;
	private Gender gender;
	private String birthYear;
	private Boolean isPrivateInformAgreed;
}
