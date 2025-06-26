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

    List<CareerNews> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    List<CareerNews> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT n FROM CareerNews n ORDER BY n.createdAt DESC")
    List<CareerNews> findPersonalizedNews(Pageable pageable);

    @Query("SELECT n FROM CareerNews n WHERE n.userInterests LIKE %:interest% ORDER BY n.createdAt DESC")
    List<CareerNews> findByInterestContaining(@Param("interest") String interest, Pageable pageable);

    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT n FROM CareerNews n WHERE (:category IS NULL OR n.category = :category) ORDER BY n.createdAt DESC")
    List<CareerNews> findNewsByCategory(@Param("category") String category, Pageable pageable);

    long count();

    List<CareerNews> findByTitleContainingOrOriginalContentContainingOrderByCreatedAtDesc(
        String titleKeyword, String contentKeyword, Pageable pageable);

    List<CareerNews> findByUserInterests(String userInterests, Pageable pageable);
}
