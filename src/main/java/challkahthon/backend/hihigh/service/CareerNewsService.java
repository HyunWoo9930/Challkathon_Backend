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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerNewsService {

    private final CareerNewsRepository careerNewsRepository;
    private final UserRepository userRepository;
    private final PersonalizedCrawlerService personalizedCrawlerService;

    @Transactional(readOnly = true)
    public List<CareerNews> getPersonalizedNews(String username, String category, int size) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        Pageable pageable = PageRequest.of(0, size);

        List<CareerNews> allNews;
        if (category != null && !category.trim().isEmpty()) {
            allNews = careerNewsRepository.findNewsByCategory(category, pageable);
        } else {
            allNews = careerNewsRepository.findPersonalizedNews(pageable);
        }

        // Filter news based on user interests
        if (user.getInterests() != null && !user.getInterests().trim().isEmpty()) {
            String[] interests = user.getInterests().split("[,，]");
            return allNews.stream()
                .filter(news -> {
                    if (news.getUserInterests() == null) return false;
                    for (String interest : interests) {
                        if (news.getUserInterests().toLowerCase().contains(interest.toLowerCase().trim())) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
        }

        return allNews;
    }

    @Transactional(readOnly = true)
    public List<CareerNews> getLatestNewsByCategory(String category) {
        Pageable pageable = PageRequest.of(0, 20);

        if (category != null && !category.trim().isEmpty()) {
            return careerNewsRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        } else {
            return careerNewsRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    @Transactional(readOnly = true)
    public CareerNews getNewsById(Long id) {
        Optional<CareerNews> news = careerNewsRepository.findById(id);
        return news.orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CareerNews> getNewsByUserInterest(String username, String interest, int size) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        Pageable pageable = PageRequest.of(0, size);
        return careerNewsRepository.findByInterestContaining(interest, pageable);
    }

    @Transactional(readOnly = true)
    public PersonalizedNewsStats getPersonalizedNewsStats(String username) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        if (user.getInterests() == null || user.getInterests().trim().isEmpty()) {
            return PersonalizedNewsStats.builder()
                    .totalPersonalizedNews(0)
                    .recentNewsCount(0)
                    .userInterests("")
                    .build();
        }

        List<String> interests = Arrays.stream(user.getInterests().split("[,，]"))
                .map(String::trim)
                .filter(interest -> !interest.isEmpty())
                .collect(Collectors.toList());

        List<CareerNews> allNews = careerNewsRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100));

        // Filter news based on user interests
        List<CareerNews> personalizedNews = allNews.stream()
            .filter(news -> {
                if (news.getUserInterests() == null) return false;
                for (String interest : interests) {
                    if (news.getUserInterests().toLowerCase().contains(interest.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());

        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
        long recentNewsCount = personalizedNews.stream()
                .filter(news -> news.getCreatedAt() != null && news.getCreatedAt().isAfter(lastWeek))
                .count();

        return PersonalizedNewsStats.builder()
                .totalPersonalizedNews(personalizedNews.size())
                .recentNewsCount(recentNewsCount)
                .userInterests(user.getInterests())
                .build();
    }

    @Transactional
    public void triggerPersonalizedCrawling(String username) {
        personalizedCrawlerService.triggerPersonalizedCrawling(username);
        log.info("사용자 {}의 맞춤 뉴스 크롤링이 시작되었습니다", username);
    }

    @Transactional(readOnly = true)
    public List<CareerNews> searchNewsByKeyword(String keyword, int size) {
        Pageable pageable = PageRequest.of(0, size);
        return careerNewsRepository.findByTitleContainingOrOriginalContentContainingOrderByCreatedAtDesc(
            keyword, keyword, pageable);
    }

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

    @lombok.Builder
    @lombok.Data
    public static class PersonalizedNewsStats {
        private int totalPersonalizedNews;
        private long recentNewsCount;
        private String userInterests;
    }
}
