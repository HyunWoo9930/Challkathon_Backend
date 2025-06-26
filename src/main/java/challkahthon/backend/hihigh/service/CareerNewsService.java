package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerNewsService {

    private final CareerNewsRepository careerNewsRepository;
    private final UserRepository userRepository;
    private final PersonalizedCrawlerService personalizedCrawlerService;

    /**
     * 사용자별 맞춤 뉴스 조회 (기본)
     */
    @Transactional(readOnly = true)
    public List<CareerNews> getPersonalizedNews(String username, String category, int size) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        Pageable pageable = PageRequest.of(0, size);
        
        if (category != null && !category.trim().isEmpty()) {
            return careerNewsRepository.findPersonalizedNewsByCategory(user, category, pageable);
        } else {
            return careerNewsRepository.findPersonalizedNews(user, pageable);
        }
    }

    /**
     * 사용자별 고관련성 뉴스 조회
     */
    @Transactional(readOnly = true)
    public List<CareerNews> getHighRelevanceNews(String username, double minScore, int size) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        Pageable pageable = PageRequest.of(0, size);
        return careerNewsRepository.findRelevantNewsByUser(user, minScore, pageable);
    }

    /**
     * 카테고리별 최신 뉴스 조회 (기존 전체 사용자 대상)
     */
    @Transactional(readOnly = true)
    public List<CareerNews> getLatestNewsByCategory(String category) {
        Pageable pageable = PageRequest.of(0, 20);
        
        if (category != null && !category.trim().isEmpty()) {
            return careerNewsRepository.findByCategoryAndTargetUserIsNullOrderByCreatedAtDesc(category, pageable);
        } else {
            return careerNewsRepository.findByTargetUserIsNullOrderByCreatedAtDesc(pageable);
        }
    }

    /**
     * 뉴스 상세 조회
     */
    @Transactional(readOnly = true)
    public CareerNews getNewsById(Long id) {
        Optional<CareerNews> news = careerNewsRepository.findById(id);
        return news.orElse(null);
    }

    /**
     * 사용자 관심사별 뉴스 조회
     */
    @Transactional(readOnly = true)
    public List<CareerNews> getNewsByUserInterest(String username, String interest, int size) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        Pageable pageable = PageRequest.of(0, size);
        return careerNewsRepository.findByUserAndInterestContaining(user, interest, pageable);
    }

    /**
     * 사용자의 맞춤 뉴스 통계 조회
     */
    @Transactional(readOnly = true)
    public PersonalizedNewsStats getPersonalizedNewsStats(String username) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
        long recentNewsCount = careerNewsRepository.countByTargetUserAndCreatedAtAfter(user, lastWeek);
        
        List<CareerNews> recentNews = careerNewsRepository.findByTargetUserOrderByCreatedAtDesc(
                user, PageRequest.of(0, 100));
        
        long relevantNewsCount = recentNews.stream()
                .filter(news -> news.getIsRelevant() != null && news.getIsRelevant())
                .count();
        
        double avgRelevanceScore = recentNews.stream()
                .filter(news -> news.getRelevanceScore() != null)
                .mapToDouble(CareerNews::getRelevanceScore)
                .average()
                .orElse(0.0);

        return PersonalizedNewsStats.builder()
                .totalPersonalizedNews(recentNews.size())
                .recentNewsCount(recentNewsCount)
                .relevantNewsCount(relevantNewsCount)
                .averageRelevanceScore(avgRelevanceScore)
                .userInterests(user.getInterests())
                .build();
    }

    /**
     * 사용자 관심사 업데이트 시 즉시 크롤링 트리거
     */
    @Transactional
    public void triggerPersonalizedCrawling(String username) {
        personalizedCrawlerService.triggerPersonalizedCrawling(username);
        log.info("사용자 {}의 맞춤 뉴스 크롤링이 시작되었습니다", username);
    }

    /**
     * 관리자용: 전체 사용자 맞춤 크롤링 수동 실행
     */
    @Transactional
    public void triggerGlobalPersonalizedCrawling() {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getInterests() != null && !user.getInterests().trim().isEmpty())
                .toList();

        for (User user : activeUsers) {
            personalizedCrawlerService.crawlPersonalizedNewsForUser(user);
        }
        
        log.info("전체 사용자 {}명의 맞춤 뉴스 크롤링이 시작되었습니다", activeUsers.size());
    }

    /**
     * 개인화 뉴스 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class PersonalizedNewsStats {
        private int totalPersonalizedNews;
        private long recentNewsCount;
        private long relevantNewsCount;
        private double averageRelevanceScore;
        private String userInterests;
    }
}
