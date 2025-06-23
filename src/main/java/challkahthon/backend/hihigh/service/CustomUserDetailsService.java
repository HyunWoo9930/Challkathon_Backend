package challkahthon.backend.hihigh.service;

import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.domain.enums.UserRole;
import challkahthon.backend.hihigh.dto.UserUpdateDto;
import challkahthon.backend.hihigh.jwt.CustomOauth2UserDetails;
import challkahthon.backend.hihigh.jwt.OAuth2UserInfo;
import challkahthon.backend.hihigh.jwt.google.GoogleUserDetails;
import challkahthon.backend.hihigh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	public User findByUserName(String userName) {
		return userRepository.findByName(userName).orElse(null);
	}

	/**
	 * 사용자의 추가 정보(관심사, 목표, 희망직종)를 업데이트합니다.
	 * 
	 * @param userName 사용자 이름
	 * @param updateDto 업데이트할 사용자 정보
	 * @return 업데이트된 사용자 정보
	 */
	public User updateUserInfo(String userName, UserUpdateDto updateDto) {
		User user = findByUserName(userName);
		if (user != null) {
			user.setInterests(updateDto.getInterests());
			user.setGoals(updateDto.getGoals());
			user.setDesiredOccupation(updateDto.getDesiredOccupation());
			return userRepository.save(user);
		}
		return null;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		log.info("getAttributes : {}", oAuth2User.getAttributes());

		String provider = userRequest.getClientRegistration().getRegistrationId();

		OAuth2UserInfo oAuth2UserInfo = null;
		// 뒤에 진행할 다른 소셜 서비스 로그인을 위해 구분 => 구글
		if (provider.equals("google")) {
			log.info("구글 로그인");
			oAuth2UserInfo = new GoogleUserDetails(oAuth2User.getAttributes());
		}

		String providerId = oAuth2UserInfo.getProviderId();
		String email = oAuth2UserInfo.getEmail();
		String loginId = provider + "_" + providerId;
		String name = oAuth2UserInfo.getName();

		Optional<User> findMember = userRepository.findByLoginId(loginId);
		User user;

		if (findMember.isEmpty()) {
			user = User.builder()
				.loginId(loginId)
				.name(name)
				.provider(provider)
				.providerId(providerId)
				.userRole(UserRole.MEMBER)
				.build();
			userRepository.save(user);
		} else {
			user = findMember.get();
		}

		return new CustomOauth2UserDetails(user, oAuth2User.getAttributes());
	}
}
