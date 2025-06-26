package challkahthon.backend.hihigh.repository;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CareerNewsRepository extends JpaRepository<CareerNews, Long> {
    
    List<CareerNews> findByCategoryAndTargetUserIsNullOrderByCreatedAtDesc(String category, Pageable pageable);
    
    List<CareerNews> findByTargetUserIsNullOrderByCreatedAtDesc(Pageable pageable);
    
    List<CareerNews> findByTargetUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT n FROM CareerNews n WHERE n.targetUser = :user OR n.targetUser IS NULL ORDER BY n.createdAt DESC")
    List<CareerNews> findPersonalizedNews(@Param("user") User user, Pageable pageable);
    
    @Query("SELECT n FROM CareerNews n WHERE n.targetUser = :user AND n.userInterests LIKE %:interest% ORDER BY n.createdAt DESC")
    List<CareerNews> findByUserAndInterestContaining(@Param("user") User user, @Param("interest") String interest, Pageable pageable);
    
    boolean existsBySourceUrlAndTargetUser(String sourceUrl, User targetUser);
    
    boolean existsBySourceUrlAndTargetUserIsNull(String sourceUrl);
    
    long countByTargetUserAndCreatedAtAfter(User user, LocalDateTime dateTime);
    
    @Query("SELECT n FROM CareerNews n WHERE (n.targetUser = :user OR n.targetUser IS NULL) AND (:category IS NULL OR n.category = :category) ORDER BY n.createdAt DESC")
    List<CareerNews> findPersonalizedNewsByCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);
    
    long countByTargetUser(User user);
    
    List<CareerNews> findByTargetUser(User user);
    
    long countByTargetUserIsNull();
    
    List<CareerNews> findByTitleContainingOrOriginalContentContainingOrderByCreatedAtDesc(
        String titleKeyword, String contentKeyword, Pageable pageable);
}
