package challkahthon.backend.hihigh.repository;

import challkahthon.backend.hihigh.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByName(String name);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // 관심사가 설정된 사용자 조회
    @Query("SELECT u FROM User u WHERE u.interests IS NOT NULL AND u.interests != ''")
    List<User> findUsersWithInterests();
    
    // 특정 관심사를 가진 사용자 조회
    @Query("SELECT u FROM User u WHERE u.interests LIKE %:interest%")
    List<User> findByInterestsContaining(String interest);
    
    // 희망직종이 설정된 사용자 조회
    @Query("SELECT u FROM User u WHERE u.desiredOccupation IS NOT NULL AND u.desiredOccupation != ''")
    List<User> findUsersWithDesiredOccupation();
    
    // 목표가 설정된 사용자 조회
    @Query("SELECT u FROM User u WHERE u.goals IS NOT NULL AND u.goals != ''")
    List<User> findUsersWithGoals();
    
    // 완전한 프로필을 가진 사용자 조회 (관심사, 목표, 희망직종 모두 설정)
    @Query("SELECT u FROM User u WHERE u.interests IS NOT NULL AND u.interests != '' " +
           "AND u.goals IS NOT NULL AND u.goals != '' " +
           "AND u.desiredOccupation IS NOT NULL AND u.desiredOccupation != ''")
    List<User> findUsersWithCompleteProfile();
}
