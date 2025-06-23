package challkahthon.backend.hihigh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import challkahthon.backend.hihigh.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByName(String name);

	Optional<User> findByLoginId(String loginId);
}
