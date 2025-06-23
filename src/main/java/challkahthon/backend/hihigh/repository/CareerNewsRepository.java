package challkahthon.backend.hihigh.repository;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CareerNewsRepository extends JpaRepository<CareerNews, Long> {
    
    // 기존 메서드들
    List<CareerNews> findTop10ByOrderByPublishedDateDesc();
    
    List<CareerNews> findTop10ByCategoryOrderByPublishedDateDesc(String category);
    
    @Query("SELECT cn FROM CareerNews cn WHERE cn.keywords LIKE %:keyword% " +
           "OR cn.title LIKE %:keyword% OR cn.originalContent LIKE %:keyword%")
    List<CareerNews> findByKeywordsContaining(@Param("keyword") String keyword);
    
    // 새로 추가되는 메서드들
    
    /**
     * URL로 중복 체크
     */
    boolean existsBySourceUrl(String sourceUrl);
    
    /**
     * 제목과 소스로 유사한 기사 찾기 (중복 방지용)
     */
    @Query("SELECT cn FROM CareerNews cn WHERE cn.title = :title AND cn.source = :source")
    List<CareerNews> findByTitleAndSource(@Param("title") String title, @Param("source") String source);
    
    /**
     * 특정 기간 이후의 뉴스만 조회
     */
    @Query("SELECT cn FROM CareerNews cn WHERE cn.publishedDate >= :since ORDER BY cn.publishedDate DESC")
    List<CareerNews> findRecentNews(@Param("since") LocalDateTime since);
    
    /**
     * 카테고리와 기간으로 뉴스 조회
     */
    @Query("SELECT cn FROM CareerNews cn WHERE cn.category = :category " +
           "AND cn.publishedDate >= :since ORDER BY cn.publishedDate DESC")
    List<CareerNews> findRecentNewsByCategory(@Param("category") String category, 
                                            @Param("since") LocalDateTime since);
    
    /**
     * 번역되지 않은 뉴스 조회
     */
    @Query("SELECT cn FROM CareerNews cn WHERE cn.language = 'en' " +
           "AND (cn.translatedContent IS NULL OR cn.translatedContent = '')")
    List<CareerNews> findUntranslatedNews();
    
    /**
     * 요약되지 않은 뉴스 조회
     */
    @Query("SELECT cn FROM CareerNews cn WHERE cn.summary IS NULL OR cn.summary = ''")
    List<CareerNews> findUnsummarizedNews();
    
    /**
     * 소스별 뉴스 개수 조회
     */
    @Query("SELECT cn.source, COUNT(cn) FROM CareerNews cn GROUP BY cn.source")
    List<Object[]> countBySource();
    
    /**
     * 카테고리별 뉴스 개수 조회
     */
    @Query("SELECT cn.category, COUNT(cn) FROM CareerNews cn GROUP BY cn.category")
    List<Object[]> countByCategory();
    
    /**
     * 언어별 뉴스 개수 조회
     */
    @Query("SELECT cn.language, COUNT(cn) FROM CareerNews cn GROUP BY cn.language")
    List<Object[]> countByLanguage();
    
    /**
     * 특정 소스의 최신 뉴스 조회
     */
    @Query("SELECT cn FROM CareerNews cn WHERE cn.source = :source " +
           "ORDER BY cn.publishedDate DESC")
    List<CareerNews> findBySourceOrderByPublishedDateDesc(@Param("source") String source);
}