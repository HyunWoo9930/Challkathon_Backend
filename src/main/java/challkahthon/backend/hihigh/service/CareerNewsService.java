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
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerNewsService {

    private final CareerNewsRepository careerNewsRepository;
    private final WebCrawlerService webCrawlerService;
    private final TranslationService translationService;
    private final SummarizationService summarizationService;
    private final AIAnalysisService aiAnalysisService;
    
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
     * Process a single news article: AI 분석 + 제목/요약 우선 번역 + 비동기 본문 번역
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
            
            // 1. AI 분석 (카테고리 검증 및 키워드 추출)
            if (news.getIsAiAnalyzed() == null || !news.getIsAiAnalyzed()) {
                log.info("Starting AI analysis...");
                AIAnalysisService.AIAnalysisResult aiResult = aiAnalysisService.analyzeArticle(
                        news.getTitle(), 
                        news.getOriginalContent(), 
                        news.getCategory()
                );
                
                // AI 분석 결과를 엔티티에 저장
                news.setIsAiAnalyzed(true);
                news.setIsRelevant(aiResult.isRelevant());
                news.setCategoryMatch(aiResult.isCategoryMatch());
                news.setRelevanceScore(aiResult.getRelevanceScore());
                news.setSuggestedCategory(aiResult.getSuggestedCategory());
                news.setAnalysisReason(aiResult.getReason());
                
                // 키워드 저장 (기존 키워드와 AI 키워드 결합)
                List<String> aiKeywords = aiResult.getKeywords();
                if (!aiKeywords.isEmpty()) {
                    String existingKeywords = news.getKeywords() != null ? news.getKeywords() : "";
                    String combinedKeywords = combineKeywords(existingKeywords, aiKeywords);
                    news.setKeywords(combinedKeywords);
                }
                
                // AI가 제안한 카테고리가 다르고 더 적절하다면 카테고리 업데이트 고려
                if (!aiResult.isCategoryMatch() && aiResult.getRelevanceScore() > 0.7) {
                    log.info("AI suggests different category: {} -> {}", 
                            news.getCategory(), aiResult.getSuggestedCategory());
                    // 카테고리를 AI 제안으로 변경할지는 정책에 따라 결정
                    // news.setCategory(aiResult.getSuggestedCategory());
                }
                
                log.info("AI analysis completed: {}", aiResult);
            }
            
            // 2. 관련성이 낮은 기사는 처리 건너뛰기
            if (news.getIsRelevant() != null && !news.getIsRelevant()) {
                log.info("Skipping irrelevant article: {}", news.getTitle());
                news.setUpdatedAt(LocalDateTime.now());
                return careerNewsRepository.save(news);
            }
            
            // 3. 제목 번역 (동기 - 즉시 번역)
            if ("en".equals(news.getLanguage()) && news.getTitle() != null) {
                log.info("Translating title...");
                String translatedTitle = translationService.translateTitle(news.getTitle());
                if (!translatedTitle.equals(news.getTitle())) {
                    news.setTitle(translatedTitle);
                    log.info("Title translation completed");
                }
            }
            
            // 4. 요약 생성
            if (news.getSummary() == null || news.getSummary().isEmpty()) {
                log.info("Generating summary...");
                String contentToSummarize = news.getOriginalContent();
                int maxSentences = contentToSummarize.length() > 3000 ? 8 : 5;
                String summary = summarizationService.summarizeText(contentToSummarize, maxSentences);
                news.setSummary(summary);
                log.info("Summary completed. Summary length: {}", summary.length());
            }
            
            // 5. 요약 번역 (동기 - 즉시 번역)
            if ("en".equals(news.getLanguage()) && news.getSummary() != null) {
                log.info("Translating summary...");
                String translatedSummary = translationService.translateSummary(news.getSummary());
                if (!translatedSummary.equals(news.getSummary())) {
                    news.setSummary(translatedSummary);
                    log.info("Summary translation completed");
                }
            }
            
            // 6. 먼저 저장 (제목과 요약은 번역된 상태, AI 분석 완료)
            news.setUpdatedAt(LocalDateTime.now());
            CareerNews savedNews = careerNewsRepository.save(news);
            
            // 7. 비동기로 본문 번역 시작 (백그라운드에서 처리)
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
            
            log.info("News processing completed (AI analyzed, title & summary translated, content translation in progress)");
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
     * AI 분석이 되지 않은 뉴스들을 일괄 분석
     * @return 분석된 뉴스 수
     */
    @Transactional
    public int analyzeUnanalyzedNews() {
        List<CareerNews> unanalyzedNews = careerNewsRepository.findUnanalyzedNews();
        int count = 0;
        
        for (CareerNews news : unanalyzedNews) {
            try {
                log.info("Analyzing news: {}", news.getTitle().substring(0, Math.min(50, news.getTitle().length())));
                
                AIAnalysisService.AIAnalysisResult aiResult = aiAnalysisService.analyzeArticle(
                        news.getTitle(), 
                        news.getOriginalContent(), 
                        news.getCategory()
                );
                
                // AI 분석 결과 저장
                news.setIsAiAnalyzed(true);
                news.setIsRelevant(aiResult.isRelevant());
                news.setCategoryMatch(aiResult.isCategoryMatch());
                news.setRelevanceScore(aiResult.getRelevanceScore());
                news.setSuggestedCategory(aiResult.getSuggestedCategory());
                news.setAnalysisReason(aiResult.getReason());
                
                // 키워드 업데이트
                List<String> aiKeywords = aiResult.getKeywords();
                if (!aiKeywords.isEmpty()) {
                    String existingKeywords = news.getKeywords() != null ? news.getKeywords() : "";
                    String combinedKeywords = combineKeywords(existingKeywords, aiKeywords);
                    news.setKeywords(combinedKeywords);
                }
                
                news.setUpdatedAt(LocalDateTime.now());
                careerNewsRepository.save(news);
                count++;
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("Error analyzing news (ID: {}): {}", news.getId(), e.getMessage());
            }
        }
        
        log.info("AI analysis completed for {} news articles", count);
        return count;
    }
    
    /**
     * 카테고리 자동 재분류
     * @return 재분류된 뉴스 수
     */
    @Transactional
    public int autoReclassifyCategories() {
        List<CareerNews> allNews = careerNewsRepository.findAll();
        int count = 0;
        
        for (CareerNews news : allNews) {
            try {
                String suggestedCategory = aiAnalysisService.autoClassifyCategory(
                        news.getTitle(), 
                        news.getOriginalContent()
                );
                
                if (!suggestedCategory.equals(news.getCategory())) {
                    log.info("Reclassifying news: {} -> {}", news.getCategory(), suggestedCategory);
                    news.setSuggestedCategory(suggestedCategory);
                    news.setUpdatedAt(LocalDateTime.now());
                    careerNewsRepository.save(news);
                    count++;
                }
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("Error reclassifying news (ID: {}): {}", news.getId(), e.getMessage());
            }
        }
        
        log.info("Category reclassification completed for {} news articles", count);
        return count;
    }
    
    /**
     * 키워드 재추출
     * @return 키워드가 업데이트된 뉴스 수
     */
    @Transactional
    public int reextractKeywords() {
        List<CareerNews> allNews = careerNewsRepository.findAll();
        int count = 0;
        
        for (CareerNews news : allNews) {
            try {
                List<String> newKeywords = aiAnalysisService.extractKeywords(
                        news.getTitle(), 
                        news.getOriginalContent()
                );
                
                if (!newKeywords.isEmpty()) {
                    String keywordsString = String.join(", ", newKeywords);
                    news.setKeywords(keywordsString);
                    news.setUpdatedAt(LocalDateTime.now());
                    careerNewsRepository.save(news);
                    count++;
                }
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("Error extracting keywords for news (ID: {}): {}", news.getId(), e.getMessage());
            }
        }
        
        log.info("Keyword extraction completed for {} news articles", count);
        return count;
    }
    
    /**
     * AI 분석 통계 조회
     */
    public Map<String, Object> getAIAnalysisStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 전체 분석 통계
            Object[] analysisStats = careerNewsRepository.getAnalysisStatistics();
            if (analysisStats != null && analysisStats.length >= 4) {
                stats.put("totalAnalyzed", ((Number) analysisStats[0]).longValue());
                stats.put("totalRelevant", ((Number) analysisStats[1]).longValue());
                stats.put("totalCategoryMatched", ((Number) analysisStats[2]).longValue());
                stats.put("totalArticles", ((Number) analysisStats[3]).longValue());
            }
            
            // 제안 카테고리별 통계
            List<Object[]> suggestedCategoryStats = careerNewsRepository.countBySuggestedCategory();
            Map<String, Long> suggestedCategories = new HashMap<>();
            for (Object[] result : suggestedCategoryStats) {
                suggestedCategories.put((String) result[0], ((Number) result[1]).longValue());
            }
            stats.put("suggestedCategories", suggestedCategories);
            
            // 관련성 점수 분포
            List<CareerNews> relevantNews = careerNewsRepository.findRelevantNews();
            Map<String, Integer> scoreDistribution = new HashMap<>();
            scoreDistribution.put("high (0.8-1.0)", 0);
            scoreDistribution.put("medium (0.5-0.8)", 0);
            scoreDistribution.put("low (0.0-0.5)", 0);
            
            for (CareerNews news : relevantNews) {
                if (news.getRelevanceScore() != null) {
                    double score = news.getRelevanceScore();
                    if (score >= 0.8) {
                        scoreDistribution.put("high (0.8-1.0)", scoreDistribution.get("high (0.8-1.0)") + 1);
                    } else if (score >= 0.5) {
                        scoreDistribution.put("medium (0.5-0.8)", scoreDistribution.get("medium (0.5-0.8)") + 1);
                    } else {
                        scoreDistribution.put("low (0.0-0.5)", scoreDistribution.get("low (0.0-0.5)") + 1);
                    }
                }
            }
            stats.put("relevanceScoreDistribution", scoreDistribution);
            
        } catch (Exception e) {
            log.error("Error getting AI analysis statistics: {}", e.getMessage());
            stats.put("error", "통계 조회 중 오류 발생: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 관련성이 높은 뉴스만 조회
     */
    public List<CareerNews> getHighRelevanceNews(double minScore) {
        return careerNewsRepository.findHighRelevanceNews(minScore);
    }
    
    /**
     * 카테고리가 일치하는 뉴스만 조회
     */
    public List<CareerNews> getCategoryMatchedNews(String category) {
        return careerNewsRepository.findCategoryMatchedNews(category);
    }
    
    /**
     * 향상된 키워드 검색
     */
    public List<CareerNews> searchByKeywordEnhanced(String keyword) {
        return careerNewsRepository.findByKeywordEnhanced(keyword);
    }
    
    /**
     * 기존 키워드와 AI 키워드를 결합
     */
    private String combineKeywords(String existingKeywords, List<String> aiKeywords) {
        Set<String> keywordSet = new HashSet<>();
        
        // 기존 키워드 추가
        if (existingKeywords != null && !existingKeywords.trim().isEmpty()) {
            String[] existing = existingKeywords.split(",");
            for (String keyword : existing) {
                String trimmed = keyword.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    keywordSet.add(trimmed);
                }
            }
        }
        
        // AI 키워드 추가
        for (String aiKeyword : aiKeywords) {
            String trimmed = aiKeyword.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                keywordSet.add(trimmed);
            }
        }
        
        // 최대 10개로 제한하고 결합
        return keywordSet.stream()
                .limit(10)
                .collect(Collectors.joining(", "));
    }
    
    /**
     * AI 분석 테스트
     */
    public Map<String, Object> testAIAnalysis(String title, String content, String category) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            AIAnalysisService.AIAnalysisResult aiResult = aiAnalysisService.analyzeArticle(title, content, category);
            
            result.put("title", title);
            result.put("content", content.substring(0, Math.min(200, content.length())) + "...");
            result.put("targetCategory", category);
            result.put("isRelevant", aiResult.isRelevant());
            result.put("categoryMatch", aiResult.isCategoryMatch());
            result.put("relevanceScore", aiResult.getRelevanceScore());
            result.put("suggestedCategory", aiResult.getSuggestedCategory());
            result.put("keywords", aiResult.getKeywords());
            result.put("reason", aiResult.getReason());
            result.put("status", "success");
            
        } catch (Exception e) {
            log.error("AI analysis test failed: {}", e.getMessage());
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
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