package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizedCrawlerService {
    
    private final UserRepository userRepository;
    private final CareerNewsRepository careerNewsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${news.api.key}")
    private String newsApiKey;
    
    @Value("${gnews.api.key}")
    private String gNewsApiKey;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void crawlPersonalizedNewsForAllUsers() {
        log.info("=== 일일 개인화 뉴스 크롤링 시작 ===");
        
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getInterests() != null && !user.getInterests().trim().isEmpty())
                .toList();
        
        log.info("관심사가 설정된 사용자 {}명 발견", activeUsers.size());
        
        for (User user : activeUsers) {
            try {
                crawlPersonalizedNewsForUser(user);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("사용자 {}의 맞춤 뉴스 크롤링 실패: {}", user.getLoginId(), e.getMessage());
            }
        }
        
        log.info("=== 일일 개인화 뉴스 크롤링 완료 ===");
    }
    
    @Async
    @Transactional
    public CompletableFuture<Void> crawlPersonalizedNewsForUser(User user) {
        log.info("사용자 {}의 맞춤 뉴스 크롤링 시작", user.getLoginId());
        
        if (user.getInterests() == null || user.getInterests().trim().isEmpty()) {
            log.warn("사용자 {}의 관심사가 설정되지 않음", user.getLoginId());
            return CompletableFuture.completedFuture(null);
        }
        
        List<String> searchQueries = generateSearchQueries(user.getInterests());
        List<CareerNews> collectedNews = new ArrayList<>();
        
        for (String query : searchQueries) {
            try {
                collectedNews.addAll(crawlFromNewsAPI(query, user));
                collectedNews.addAll(crawlFromGNewsAPI(query, user));
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("쿼리 '{}'로 뉴스 수집 실패: {}", query, e.getMessage());
            }
        }
        
        List<CareerNews> uniqueNews = removeDuplicates(collectedNews);
        
        for (CareerNews news : uniqueNews) {
            try {
                if (!careerNewsRepository.existsBySourceUrlAndTargetUser(news.getSourceUrl(), user)) {
                    careerNewsRepository.save(news);
                }
            } catch (Exception e) {
                log.error("뉴스 저장 실패: {}", e.getMessage());
            }
        }
        
        log.info("사용자 {}의 맞춤 뉴스 {}개 수집 완료", user.getLoginId(), uniqueNews.size());
        return CompletableFuture.completedFuture(null);
    }
    
    private List<String> generateSearchQueries(String interests) {
        List<String> queries = new ArrayList<>();
        
        String[] keywords = interests.toLowerCase().split("[,\\s]+");
        for (String keyword : keywords) {
            if (keyword.trim().length() > 2) {
                queries.add(keyword.trim() + " developer OR " + keyword.trim() + " programming");
            }
        }
        
        if (queries.isEmpty()) {
            queries.add("software developer");
        }
        
        return queries.subList(0, Math.min(3, queries.size()));
    }
    
    private List<CareerNews> crawlFromNewsAPI(String query, User user) {
        List<CareerNews> newsList = new ArrayList<>();
        
        try {
            String url = String.format(
                "https://newsapi.org/v2/everything?q=%s&language=en&sortBy=publishedAt&pageSize=3&apiKey=%s",
                query, newsApiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode articles = root.path("articles");
            
            for (JsonNode article : articles) {
                try {
                    CareerNews news = CareerNews.builder()
                            .title(article.path("title").asText())
                            .sourceUrl(article.path("url").asText())
                            .source(article.path("source").path("name").asText())
                            .thumbnailUrl(article.path("urlToImage").asText())
                            .originalContent(article.path("description").asText())
                            .language("en")
                            .targetUser(user)
                            .userInterests(user.getInterests())
                            .publishedDate(parseDateTime(article.path("publishedAt").asText()))
                            .createdAt(LocalDateTime.now())

                            .build();
                    
                    newsList.add(news);
                } catch (Exception e) {
                    log.warn("News API 기사 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("News API 요청 실패: {}", e.getMessage());
        }
        
        return newsList;
    }
    
    private List<CareerNews> crawlFromGNewsAPI(String query, User user) {
        List<CareerNews> newsList = new ArrayList<>();
        
        try {
            String url = String.format(
                "https://gnews.io/api/v4/search?q=%s&lang=en&max=3&token=%s",
                query, gNewsApiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode articles = root.path("articles");
            
            for (JsonNode article : articles) {
                try {
                    CareerNews news = CareerNews.builder()
                            .title(article.path("title").asText())
                            .sourceUrl(article.path("url").asText())
                            .source(article.path("source").path("name").asText())
                            .thumbnailUrl(article.path("image").asText())
                            .originalContent(article.path("description").asText())
                            .language("en")
                            .targetUser(user)
                            .userInterests(user.getInterests())
                            .publishedDate(parseDateTime(article.path("publishedAt").asText()))
                            .createdAt(LocalDateTime.now())

                            .build();
                    
                    newsList.add(news);
                } catch (Exception e) {
                    log.warn("GNews API 기사 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("GNews API 요청 실패: {}", e.getMessage());
        }
        
        return newsList;
    }
    
    private List<CareerNews> removeDuplicates(List<CareerNews> newsList) {
        Map<String, CareerNews> uniqueNewsMap = new LinkedHashMap<>();
        
        for (CareerNews news : newsList) {
            if (news.getSourceUrl() != null && !uniqueNewsMap.containsKey(news.getSourceUrl())) {
                uniqueNewsMap.put(news.getSourceUrl(), news);
            }
        }
        
        return new ArrayList<>(uniqueNewsMap.values());
    }
    
    @Async
    @Transactional
    public CompletableFuture<Void> triggerPersonalizedCrawling(String username) {
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            return crawlPersonalizedNewsForUser(user);
        } catch (Exception e) {
            log.error("즉시 크롤링 실행 실패: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
                return LocalDateTime.parse(dateTimeStr.replace("Z", ""), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateTimeStr);
        }
        return LocalDateTime.now();
    }
}
