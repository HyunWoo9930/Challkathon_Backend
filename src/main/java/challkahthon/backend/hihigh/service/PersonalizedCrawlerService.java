package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import challkahthon.backend.hihigh.utils.InterestParsingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    private final InterestParsingUtils interestParsingUtils;
    private final TranslationService translationService;
    private final AIAnalysisService aiAnalysisService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${news.api.key}")
    private String newsApiKey;
    
    @Value("${gnews.api.key}")
    private String gNewsApiKey;
    
    @Value("${crawling.max-articles-per-source:5}")
    private int maxArticlesPerSource;
    
    @Value("${crawling.request-interval:2}")
    private int requestInterval;
    
    /**
     * 모든 사용자를 대상으로 맞춤 뉴스 크롤링 (스케줄링)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    @Transactional
    public void crawlPersonalizedNewsForAllUsers() {
        log.info("개인화 뉴스 크롤링 시작");
        
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getInterests() != null && !user.getInterests().trim().isEmpty())
                .toList();
        
        log.info("관심사가 설정된 사용자 {}명 발견", activeUsers.size());
        
        for (User user : activeUsers) {
            try {
                crawlPersonalizedNewsForUser(user);
                Thread.sleep(requestInterval * 1000); // API 요청 간격 조절
            } catch (Exception e) {
                log.error("사용자 {}의 맞춤 뉴스 크롤링 실패: {}", user.getLoginId(), e.getMessage());
            }
        }
        
        log.info("개인화 뉴스 크롤링 완료");
    }
    
    /**
     * 특정 사용자를 위한 맞춤 뉴스 크롤링
     */
    @Async
    @Transactional
    public CompletableFuture<Void> crawlPersonalizedNewsForUser(User user) {
        log.info("사용자 {}의 맞춤 뉴스 크롤링 시작", user.getLoginId());
        
        if (user.getInterests() == null || user.getInterests().trim().isEmpty()) {
            log.warn("사용자 {}의 관심사가 설정되지 않음", user.getLoginId());
            return CompletableFuture.completedFuture(null);
        }
        
        List<String> searchQueries = interestParsingUtils.generateSearchQueries(user.getInterests());
        List<CareerNews> collectedNews = new ArrayList<>();
        
        // 각 검색 쿼리로 뉴스 수집
        for (String query : searchQueries) {
            try {
                // News API에서 수집
                List<CareerNews> newsApiResults = crawlFromNewsAPI(query, user);
                collectedNews.addAll(newsApiResults);
                
                // GNews API에서 수집
                List<CareerNews> gNewsResults = crawlFromGNewsAPI(query, user);
                collectedNews.addAll(gNewsResults);
                
                Thread.sleep(1000); // API 요청 간격
            } catch (Exception e) {
                log.error("쿼리 '{}'로 뉴스 수집 실패: {}", query, e.getMessage());
            }
        }
        
        // 중복 제거 및 관련성 분석
        List<CareerNews> uniqueNews = removeDuplicatesAndAnalyze(collectedNews, user);
        
        // 데이터베이스 저장
        for (CareerNews news : uniqueNews) {
            try {
                if (!careerNewsRepository.existsBySourceUrlAndTargetUser(news.getSourceUrl(), user)) {
                    careerNewsRepository.save(news);
                    log.debug("사용자 {}용 뉴스 저장: {}", user.getLoginId(), news.getTitle());
                }
            } catch (Exception e) {
                log.error("뉴스 저장 실패: {}", e.getMessage());
            }
        }
        
        log.info("사용자 {}의 맞춤 뉴스 {}개 수집 완료", user.getLoginId(), uniqueNews.size());
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * News API에서 뉴스 수집
     */
    private List<CareerNews> crawlFromNewsAPI(String query, User user) {
        List<CareerNews> newsList = new ArrayList<>();
        
        try {
            String url = String.format(
                "https://newsapi.org/v2/everything?q=%s&language=en&sortBy=publishedAt&pageSize=%d&apiKey=%s",
                query, maxArticlesPerSource, newsApiKey
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
                            .isAiAnalyzed(false)
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
    
    /**
     * GNews API에서 뉴스 수집
     */
    private List<CareerNews> crawlFromGNewsAPI(String query, User user) {
        List<CareerNews> newsList = new ArrayList<>();
        
        try {
            String url = String.format(
                "https://gnews.io/api/v4/search?q=%s&lang=en&max=%d&token=%s",
                query, maxArticlesPerSource, gNewsApiKey
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
                            .isAiAnalyzed(false)
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
    
    /**
     * 중복 제거 및 AI 분석
     */
    private List<CareerNews> removeDuplicatesAndAnalyze(List<CareerNews> newsList, User user) {
        // URL 기준 중복 제거
        Map<String, CareerNews> uniqueNewsMap = new LinkedHashMap<>();
        
        for (CareerNews news : newsList) {
            if (news.getSourceUrl() != null && !uniqueNewsMap.containsKey(news.getSourceUrl())) {
                uniqueNewsMap.put(news.getSourceUrl(), news);
            }
        }
        
        List<CareerNews> uniqueNews = new ArrayList<>(uniqueNewsMap.values());
        
        // AI 분석 및 번역
        for (CareerNews news : uniqueNews) {
            try {
                // 관련성 점수 계산
                double relevanceScore = interestParsingUtils.calculateRelevanceScore(
                    news.getTitle() + " " + news.getOriginalContent(), 
                    user.getInterests()
                );
                
                news.setRelevanceScore(relevanceScore);
                news.setIsRelevant(relevanceScore >= 0.3); // 30% 이상 관련성이 있다고 판단
                
                // 영어 뉴스 번역
                if ("en".equals(news.getLanguage()) && news.getTitle() != null) {
                    String translatedTitle = translationService.translateToKorean(news.getTitle());
                    if (translatedTitle != null) {
                        news.setTranslatedContent(translatedTitle);
                    }
                }
                
                // 카테고리 추천
                Set<String> categories = interestParsingUtils.categorizeInterests(user.getInterests());
                if (!categories.isEmpty()) {
                    news.setCategory(categories.iterator().next()); // 첫 번째 카테고리 사용
                }
                
                news.setIsAiAnalyzed(true);
                
            } catch (Exception e) {
                log.warn("뉴스 분석 실패: {}", e.getMessage());
            }
        }
        
        // 관련성 점수로 정렬 (높은 순)
        uniqueNews.sort((a, b) -> Double.compare(
            b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
            a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
        ));
        
        // 상위 N개만 반환
        return uniqueNews.stream()
                .limit(maxArticlesPerSource * 2) // 소스별 최대 개수의 2배
                .toList();
    }
    
    /**
     * 사용자 관심사 업데이트 시 즉시 뉴스 크롤링
     */
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
