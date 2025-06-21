package challkahthon.backend.hihigh.service;

import org.springframework.stereotype.Service;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

	private final UserRepository userRepository;

	public User findByUserName(String userName) {
		return userRepository.findByName(userName).orElse(null);
	}
}
