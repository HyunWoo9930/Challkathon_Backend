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
    
    // 기존 전체 사용자 대상 뉴스 조회 (카테고리별)
    List<CareerNews> findByCategoryAndTargetUserIsNullOrderByCreatedAtDesc(String category, Pageable pageable);
    
    // 전체 사용자 대상 최신 뉴스 조회
    List<CareerNews> findByTargetUserIsNullOrderByCreatedAtDesc(Pageable pageable);
    
    // 특정 사용자를 위한 맞춤 뉴스 조회
    List<CareerNews> findByTargetUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 사용자별 + 전체 뉴스 통합 조회 (최신순)
    @Query("SELECT n FROM CareerNews n WHERE n.targetUser = :user OR n.targetUser IS NULL ORDER BY n.createdAt DESC")
    List<CareerNews> findPersonalizedNews(@Param("user") User user, Pageable pageable);
    
    // 특정 사용자의 관심사로 필터링된 뉴스
    @Query("SELECT n FROM CareerNews n WHERE n.targetUser = :user AND n.userInterests LIKE %:interest% ORDER BY n.createdAt DESC")
    List<CareerNews> findByUserAndInterestContaining(@Param("user") User user, @Param("interest") String interest, Pageable pageable);
    
    // 중복 방지를 위한 URL 체크
    boolean existsBySourceUrlAndTargetUser(String sourceUrl, User targetUser);
    
    // 전체 뉴스 중복 체크
    boolean existsBySourceUrlAndTargetUserIsNull(String sourceUrl);
    
    // 특정 기간 이후의 사용자별 뉴스 개수
    long countByTargetUserAndCreatedAtAfter(User user, LocalDateTime dateTime);
    
    // 카테고리별 + 사용자별 뉴스 조회
    @Query("SELECT n FROM CareerNews n WHERE (n.targetUser = :user OR n.targetUser IS NULL) AND (:category IS NULL OR n.category = :category) ORDER BY n.createdAt DESC")
    List<CareerNews> findPersonalizedNewsByCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);
    
    // AI 분석이 완료된 관련성 높은 뉴스 조회
    @Query("SELECT n FROM CareerNews n WHERE n.targetUser = :user AND n.isAiAnalyzed = true AND n.isRelevant = true AND n.relevanceScore >= :minScore ORDER BY n.relevanceScore DESC, n.createdAt DESC")
    List<CareerNews> findRelevantNewsByUser(@Param("user") User user, @Param("minScore") Double minScore, Pageable pageable);
    
    // 사용자별 뉴스 개수 조회
    long countByTargetUser(User user);
    
    // 특정 사용자의 모든 뉴스 조회
    List<CareerNews> findByTargetUser(User user);
    
    // 전체 사용자 대상 뉴스 개수 조회
    long countByTargetUserIsNull();
    
    // 키워드로 뉴스 검색 (제목 + 내용)
    List<CareerNews> findByTitleContainingOrOriginalContentContainingOrderByCreatedAtDesc(
        String titleKeyword, String contentKeyword, Pageable pageable);
}