package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerNewsService {

    private final CareerNewsRepository careerNewsRepository;
    private final WebCrawlerService webCrawlerService;
    private final TranslationService translationService;
    private final SummarizationService summarizationService;
    
    /**
     * Get the latest career news by category
     * @param category The category of news to retrieve
     * @return List of career news
     */
    public List<CareerNews> getLatestNewsByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return careerNewsRepository.findTop10ByOrderByPublishedDateDesc();
        }
        return careerNewsRepository.findTop10ByCategoryOrderByPublishedDateDesc(category);
    }
    
    /**
     * Get career news by ID
     * @param id The ID of the news to retrieve
     * @return The career news, or null if not found
     */
    public CareerNews getNewsById(Long id) {
        return careerNewsRepository.findById(id).orElse(null);
    }
    
    /**
     * Search career news by keyword
     * @param keyword The keyword to search for
     * @return List of career news containing the keyword
     */
    public List<CareerNews> searchNewsByKeyword(String keyword) {
        return careerNewsRepository.findByKeywordsContaining(keyword);
    }
    
    /**
     * Process a single news article: 제목/요약 우선 번역 + 비동기 본문 번역
     * @param news The news article to process
     * @return The processed news article
     */
    @Transactional
    public CareerNews processNews(CareerNews news) {
        if (news == null) {
            return null;
        }
        
        try {
            log.info("Processing news: {} (content length: {})", 
                    news.getTitle().substring(0, Math.min(50, news.getTitle().length())), 
                    news.getOriginalContent().length());
            
            // 1. 제목 번역 (동기 - 즉시 번역)
            if ("en".equals(news.getLanguage()) && news.getTitle() != null) {
                log.info("Translating title...");
                String translatedTitle = translationService.translateTitle(news.getTitle());
                if (!translatedTitle.equals(news.getTitle())) {
                    news.setTitle(translatedTitle);
                    log.info("Title translation completed");
                }
            }
            
            // 2. 요약 생성
            if (news.getSummary() == null || news.getSummary().isEmpty()) {
                log.info("Generating summary...");
                String contentToSummarize = news.getOriginalContent();
                int maxSentences = contentToSummarize.length() > 3000 ? 8 : 5;
                String summary = summarizationService.summarizeText(contentToSummarize, maxSentences);
                news.setSummary(summary);
                log.info("Summary completed. Summary length: {}", summary.length());
            }
            
            // 3. 요약 번역 (동기 - 즉시 번역)
            if ("en".equals(news.getLanguage()) && news.getSummary() != null) {
                log.info("Translating summary...");
                String translatedSummary = translationService.translateSummary(news.getSummary());
                if (!translatedSummary.equals(news.getSummary())) {
                    news.setSummary(translatedSummary);
                    log.info("Summary translation completed");
                }
            }
            
            // 4. 먼저 저장 (제목과 요약은 번역된 상태)
            news.setUpdatedAt(LocalDateTime.now());
            CareerNews savedNews = careerNewsRepository.save(news);
            
            // 5. 비동기로 본문 번역 시작 (백그라운드에서 처리)
            if ("en".equals(news.getLanguage()) && news.getOriginalContent() != null &&
                (news.getTranslatedContent() == null || news.getTranslatedContent().isEmpty())) {
                
                log.info("Starting async content translation...");
                CompletableFuture<String> translationFuture = translationService.translateContentAsync(news.getOriginalContent());
                
                // 비동기 완료 후 DB 업데이트
                translationFuture.thenAccept(translatedContent -> {
                    try {
                        savedNews.setTranslatedContent(translatedContent);
                        savedNews.setUpdatedAt(LocalDateTime.now());
                        careerNewsRepository.save(savedNews);
                        log.info("Async content translation completed and saved");
                    } catch (Exception e) {
                        log.error("Error saving async translation: {}", e.getMessage());
                    }
                });
            }
            
            log.info("News processing completed (title & summary translated, content translation in progress)");
            return savedNews;
            
        } catch (Exception e) {
            log.error("Error processing news (ID: {}): {}", news.getId(), e.getMessage(), e);
            return news;
        }
    }
    
    /**
     * Crawl and process career news - 번역 없이 수집만
     * @param category The category of news to crawl
     * @return Number of news processed
     */
    @Transactional
    public int crawlAndProcessNews(String category) {
        List<CareerNews> newsList = webCrawlerService.crawlAllSources(category);
        int count = 0;
        
        for (CareerNews news : newsList) {
            try {
                // 중복 체크
                if (!careerNewsRepository.existsBySourceUrl(news.getSourceUrl())) {
                    // Save the news first
                    CareerNews savedNews = careerNewsRepository.save(news);
                    // Process (only summarization, no translation)
                    processNews(savedNews);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error processing news: {}", e.getMessage());
            }
        }
        
        return count;
    }
    
    /**
     * Scheduled task to crawl and process news daily
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void scheduledCrawlAndProcess() {
        log.info("Starting scheduled news crawling and processing");
        int count = crawlAndProcessNews(null); // Crawl all categories
        log.info("Completed scheduled news crawling and processing. Processed {} news articles", count);
    }
    
    /**
     * Get all available categories
     * @return List of categories
     */
    public List<String> getAllCategories() {
        return List.of("frontend", "backend", "design", "planning", "devops");
    }
    
    /**
     * Process all unprocessed news (missing translation or summary)
     * @return Number of news processed
     */
    @Transactional
    public int processAllUnprocessedNews() {
        List<CareerNews> allNews = careerNewsRepository.findAll();
        int count = 0;
        
        for (CareerNews news : allNews) {
            boolean needsProcessing = false;
            
            // 번역이 필요한 경우 (영어 기사이면서 번역이 안된 경우)
            if ("en".equals(news.getLanguage()) && 
                (news.getTranslatedContent() == null || news.getTranslatedContent().trim().isEmpty())) {
                needsProcessing = true;
            }
            
            // 요약이 없는 경우
            if (news.getSummary() == null || news.getSummary().trim().isEmpty()) {
                needsProcessing = true;
            }
            
            // 제목이 번역 안된 경우 (영어 제목 그대로인 경우)
            if ("en".equals(news.getLanguage()) && news.getTitle() != null && 
                !containsKorean(news.getTitle())) {
                needsProcessing = true;
            }
            
            if (needsProcessing) {
                try {
                    processNews(news);
                    count++;
                } catch (Exception e) {
                    log.error("Error processing news: {}", e.getMessage());
                }
            }
        }
        
        return count;
    }
    
    /**
     * 텍스트에 한글이 포함되어 있는지 확인
     */
    private boolean containsKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7AF) { // 한글 유니코드 범위
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get source statistics
     * @return Map of source name to count
     */
    public Map<String, Long> getSourceStatistics() {
        List<Object[]> results = careerNewsRepository.countBySource();
        Map<String, Long> stats = new HashMap<>();
        
        for (Object[] result : results) {
            stats.put((String) result[0], ((Number) result[1]).longValue());
        }
        
        return stats;
    }
    
    /**
     * 현재 설정된 뉴스 소스 정보 조회
     */
    public Map<String, Object> getConfiguredSources() {
        Map<String, Object> sources = new HashMap<>();
        
        // RSS 소스들
        Map<String, String> rssSources = new HashMap<>();
        
        // 개발자/디자이너 전용 소스 (5개)
        rssSources.put("Dev.to", "https://dev.to/feed");
        rssSources.put("Medium Tech", "https://medium.com/feed/topic/technology");
        rssSources.put("CSS-Tricks", "https://css-tricks.com/feed");
        rssSources.put("Smashing Magazine", "https://www.smashingmagazine.com/feed");
        rssSources.put("A List Apart", "https://alistapart.com/main/feed");
        
        sources.put("rssSources", rssSources);
        sources.put("newsAPI", "NewsAPI.org - 글로벌 뉴스 검색 (3개 기사)");
        sources.put("gNewsAPI", "GNews.io - 구글 뉴스 검색 (3개 기사)");
        
        // 언어별 소스 분류
        Map<String, List<String>> languageSources = new HashMap<>();
        List<String> devSources = List.of("Dev.to", "Medium Tech", "CSS-Tricks", "Smashing Magazine", "A List Apart");
        
        languageSources.put("개발자 전용 영어", devSources);
        sources.put("languageClassification", languageSources);
        
        // 수집 정보
        sources.put("totalRSSSources", rssSources.size());
        sources.put("articlesPerRSSSource", 2);
        sources.put("articlesPerAPISource", 1);
        sources.put("estimatedTotalArticles", rssSources.size() * 2); // RSS(5×2) = 10개
        sources.put("lastUpdated", LocalDateTime.now());
        
        return sources;
    }
    
    /**
     * Get category statistics
     * @return Map of category name to count
     */
    public Map<String, Long> getCategoryStatistics() {
        List<Object[]> results = careerNewsRepository.countByCategory();
        Map<String, Long> stats = new HashMap<>();
        
        for (Object[] result : results) {
            stats.put((String) result[0], ((Number) result[1]).longValue());
        }
        
        return stats;
    }
    
    /**
     * 번역 서비스 테스트 (제목/요약 우선)
     */
    public String testTranslation(String text) {
        try {
            if (text.length() <= 200) {
                return translationService.translateTitle(text);
            } else {
                return translationService.translateSummary(text.substring(0, Math.min(500, text.length())));
            }
        } catch (Exception e) {
            log.error("Translation test failed: {}", e.getMessage());
            return "Translation test failed: " + e.getMessage();
        }
    }
    
    /**
     * 특정 URL 크롤링 테스트
     */
    public Map<String, Object> testCrawlUrl(String url, String sourceName) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("Testing URL crawling for: {} (source: {})", url, sourceName);

            // WebCrawlerService의 private 메서드를 테스트하기 위해 임시로 public 메서드 추가 필요
            // 또는 reflection 사용하지만, 여기서는 간단히 새로운 테스트 전용 메서드 추가
            String content = webCrawlerService.testCrawlSingleUrl(url, sourceName);

            result.put("url", url);
            result.put("sourceName", sourceName);
            result.put("contentLength", content != null ? content.length() : 0);
            result.put("content", content);
            result.put("success", content != null && !content.isEmpty());
            result.put("timestamp", LocalDateTime.now());

            if (content != null && content.length() > 200) {
                result.put("preview", content.substring(0, 200) + "...");
            } else {
                result.put("preview", content);
            }

        } catch (Exception e) {
            log.error("Error testing URL crawl: {}", e.getMessage());
            result.put("url", url);
            result.put("sourceName", sourceName);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        return result;
    }
        
    /**
     * Get language statistics
     * @return Map of language to count
     */
    public Map<String, Long> getLanguageStatistics() {
        List<Object[]> results = careerNewsRepository.countByLanguage();
        Map<String, Long> stats = new HashMap<>();
        
        for (Object[] result : results) {
            String language = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            
            // 언어 코드를 한국어로 변환
            String displayLanguage = language.equals("ko") ? "한국어" : 
                                    language.equals("en") ? "영어" : language;
            stats.put(displayLanguage, count);
        }
        
        return stats;
    }

}