package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebCrawlerService {

    private final CareerNewsRepository careerNewsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${news.api.key:YOUR_NEWS_API_KEY}")
    private String newsApiKey;
    
    @Value("${gnews.api.key:YOUR_GNEWS_API_KEY}")
    private String gNewsApiKey;
    
    // RSS 피드 소스들 (개발자/디자이너 전용 5개) - 총 10개 기사 수집 목표
    private static final Map<String, String> RSS_SOURCES = new HashMap<>();
    
    static {
        // === 개발자/디자이너 전용 영어 사이트들 (5개) ===
        RSS_SOURCES.put("Dev.to", "https://dev.to/feed");
        RSS_SOURCES.put("Medium Tech", "https://medium.com/feed/topic/technology");
        RSS_SOURCES.put("CSS-Tricks", "https://css-tricks.com/feed");
        RSS_SOURCES.put("Smashing Magazine", "https://www.smashingmagazine.com/feed");
        RSS_SOURCES.put("A List Apart", "https://alistapart.com/main/feed");
    }
    
    /**
     * 기존 메서드 - 호환성 유지
     */
    public List<CareerNews> crawlCareerNews(String category) {
        return crawlAllSources(category);
    }
    
    /**
     * NewsAPI를 사용해서 커리어 관련 뉴스 수집 (전체 본문 크롤링 포함)
     */
    public List<CareerNews> crawlFromNewsAPI(String category) {
        List<CareerNews> newsList = new ArrayList<>();
        
        try {
            String query = buildNewsAPIQuery(category);
            String url = String.format(
                "https://newsapi.org/v2/everything?q=%s&language=en&sortBy=publishedAt&pageSize=5&apiKey=%s",
                query, newsApiKey
            );
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode articles = root.get("articles");
            
            if (articles != null && articles.isArray()) {
                int count = 0;
                for (JsonNode article : articles) {
                    if (count >= 2) break; // API당 2개로 제한 (총 영문 5개 목표)
                    
                    CareerNews news = parseNewsAPIArticle(article, category);
                    if (news != null) {
                        newsList.add(news);
                        count++;
                        Thread.sleep(2000);
                    }
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while crawling from NewsAPI");
        } catch (Exception e) {
            log.error("Error crawling from NewsAPI: {}", e.getMessage());
        }
        
        log.info("NewsAPI crawling completed: {} articles", newsList.size());
        return newsList;
    }
    
    /**
     * GNews API를 사용해서 커리어 관련 뉴스 수집 (전체 본문 크롤링 포함)
     */
    public List<CareerNews> crawlFromGNewsAPI(String category) {
        List<CareerNews> newsList = new ArrayList<>();
        
        try {
            String query = buildGNewsQuery(category);
            String url = String.format(
                "https://gnews.io/api/v4/search?q=%s&lang=en&country=us&max=1&apikey=%s",
                query, gNewsApiKey
            );
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode articles = root.get("articles");
            
            if (articles != null && articles.isArray()) {
                for (JsonNode article : articles) {
                    CareerNews news = parseGNewsArticle(article, category);
                    if (news != null) {
                        newsList.add(news);
                        Thread.sleep(2000);
                    }
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while crawling from GNews API");
        } catch (Exception e) {
            log.error("Error crawling from GNews API: {}", e.getMessage());
        }
        
        log.info("GNews API crawling completed: {} articles", newsList.size());
        return newsList;
    }
    
    /**
     * RSS 피드에서 뉴스 수집 (전체 본문 크롤링 포함)
     */
    public List<CareerNews> crawlFromRSSFeeds(String category) {
        List<CareerNews> newsList = new ArrayList<>();
        
        for (Map.Entry<String, String> source : RSS_SOURCES.entrySet()) {
            try {
                log.info("Crawling RSS from: {} - {}", source.getKey(), source.getValue());
                
                Document doc = Jsoup.connect(source.getValue())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
                    
                Elements items = doc.select("item");
                if (items.isEmpty()) {
                    items = doc.select("entry");
                }
                
                int count = 0;
                for (Element item : items) {
                    if (count >= 2) break; // 소스당 2개로 제한 - 영어 소스 5개 = 10개
                    
                    CareerNews news = parseRSSItemWithFullContent(item, source.getKey(), category);
                    if (news != null && 
                        isCareerRelated(news.getTitle() + " " + news.getOriginalContent()) &&
                        isValidArticleContent(news.getOriginalContent(), news.getTitle())) {
                        newsList.add(news);
                        count++;
                        Thread.sleep(2000);
                    } else if (news != null) {
                        log.info("Filtered out invalid/restricted content: {}", news.getTitle());
                    }
                }
                
                log.info("Successfully crawled {} articles from {}", count, source.getKey());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while crawling from {}", source.getKey());
                break;
            } catch (Exception e) {
                log.error("Error crawling RSS from {}: {}", source.getKey(), e.getMessage());
            }
        }
        
        return newsList;
    }
    
    /**
     * 모든 소스에서 뉴스 수집
     */
    public List<CareerNews> crawlAllSources(String category) {
        List<CareerNews> allNews = new ArrayList<>();
        
        log.info("=== Starting news crawling for category: {} ===", category != null ? category : "all");
        
        // NewsAPI에서 수집
        if (!"YOUR_NEWS_API_KEY".equals(newsApiKey)) {
            log.info("Crawling from NewsAPI...");
            List<CareerNews> newsApiNews = crawlFromNewsAPI(category);
            allNews.addAll(newsApiNews);
            log.info("NewsAPI: collected {} articles", newsApiNews.size());
        } else {
            log.warn("NewsAPI key not configured, skipping...");
        }
        
        // GNews API에서 수집
        if (!"YOUR_GNEWS_API_KEY".equals(gNewsApiKey)) {
            log.info("Crawling from GNews API...");
            List<CareerNews> gNewsApiNews = crawlFromGNewsAPI(category);
            allNews.addAll(gNewsApiNews);
            log.info("GNews API: collected {} articles", gNewsApiNews.size());
        } else {
            log.warn("GNews API key not configured, skipping...");
        }
        
        // RSS 피드에서 수집
        log.info("Crawling from RSS feeds...");
        List<CareerNews> rssNews = crawlFromRSSFeeds(category);
        allNews.addAll(rssNews);
        log.info("RSS feeds: collected {} articles", rssNews.size());
        
        // 중복 제거
        List<CareerNews> uniqueNews = removeDuplicates(allNews);
        int duplicatesRemoved = allNews.size() - uniqueNews.size();
        
        log.info("=== Crawling completed ===");
        log.info("Total collected: {} articles", allNews.size());
        log.info("Duplicates removed: {} articles", duplicatesRemoved);
        log.info("Final unique articles: {} articles", uniqueNews.size());
        
        return uniqueNews;
    }
    
    private String buildNewsAPIQuery(String category) {
        Map<String, String> queries = new HashMap<>();
        queries.put("frontend", "\"frontend developer\" OR \"javascript\" OR \"react\" OR \"vue\" OR \"angular\"");
        queries.put("backend", "\"backend developer\" OR \"server\" OR \"database\" OR \"API\" OR \"microservices\"");
        queries.put("design", "\"UI design\" OR \"UX design\" OR \"product design\" OR \"user experience\"");
        queries.put("planning", "\"product manager\" OR \"project management\" OR \"product planning\" OR \"roadmap\"");
        queries.put("devops", "\"devops\" OR \"cloud\" OR \"infrastructure\" OR \"deployment\" OR \"CI/CD\"");
        
        return queries.getOrDefault(category, "\"software developer\" OR \"programming jobs\" OR \"tech career\"");
    }
    
    private String buildGNewsQuery(String category) {
        Map<String, String> queries = new HashMap<>();
        queries.put("frontend", "frontend developer javascript react");
        queries.put("backend", "backend developer server API");
        queries.put("design", "UI UX design product");
        queries.put("planning", "product manager planning");
        queries.put("devops", "devops cloud infrastructure");
        
        return queries.getOrDefault(category, "software developer programming");
    }
    
    private CareerNews parseNewsAPIArticle(JsonNode article, String category) {
        try {
            String title = article.get("title").asText();
            String apiContent = article.get("content") != null ? article.get("content").asText() : 
                               article.get("description").asText();
            String url = article.get("url").asText();
            String source = article.get("source").get("name").asText();
            String publishedAt = article.get("publishedAt").asText();
            
            // 썸네일 URL 추출
            String thumbnailUrl = "";
            if (article.has("urlToImage") && !article.get("urlToImage").isNull()) {
                thumbnailUrl = article.get("urlToImage").asText();
            }
            
            String fullContent = crawlFullArticleContent(url, source);
            String finalContent = (!fullContent.isEmpty() && fullContent.length() > apiContent.length()) 
                                 ? fullContent : apiContent;
            
            // 제한된 콘텐츠인지 확인
            if (!isValidArticleContent(finalContent, title)) {
                log.warn("Skipping restricted/invalid content from GNews: {}", title);
                return null;
            }
            
            // 전체 본문에서 썸네일이 없으면 추출 시도
            if (thumbnailUrl.isEmpty() && !fullContent.isEmpty()) {
                thumbnailUrl = extractThumbnailFromContent(url, source);
            }
            
            LocalDateTime publishedDate = LocalDateTime.parse(
                publishedAt.replace("Z", ""), 
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            
            return CareerNews.builder()
                    .title(title)
                    .thumbnailUrl(thumbnailUrl)
                    .originalContent(finalContent)
                    .sourceUrl(url)
                    .source(source)
                    .category(category != null ? category : "general")
                    .language("en") // 모든 소스가 영어이므로 직접 설정
                    .publishedDate(publishedDate)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error parsing NewsAPI article: {}", e.getMessage());
            return null;
        }
    }
    
    private CareerNews parseGNewsArticle(JsonNode article, String category) {
        try {
            String title = article.get("title").asText();
            String apiContent = article.get("content") != null ? article.get("content").asText() : 
                               article.get("description").asText();
            String url = article.get("url").asText();
            String source = article.get("source").get("name").asText();
            String publishedAt = article.get("publishedAt").asText();
            
            // 썸네일 URL 추출
            String thumbnailUrl = "";
            if (article.has("image") && !article.get("image").isNull()) {
                thumbnailUrl = article.get("image").asText();
            }
            
            String fullContent = crawlFullArticleContent(url, source);
            String finalContent = (!fullContent.isEmpty() && fullContent.length() > apiContent.length()) 
                                 ? fullContent : apiContent;
            
            // 제한된 콘텐츠인지 확인
            if (!isValidArticleContent(finalContent, title)) {
                log.warn("Skipping restricted/invalid content from GNews: {}", title);
                return null;
            }
            
            // 전체 본문에서 썸네일이 없으면 추출 시도
            if (thumbnailUrl.isEmpty() && !fullContent.isEmpty()) {
                thumbnailUrl = extractThumbnailFromContent(url, source);
            }
            
            LocalDateTime publishedDate = LocalDateTime.parse(
                publishedAt.replace("Z", ""), 
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            
            return CareerNews.builder()
                    .title(title)
                    .thumbnailUrl(thumbnailUrl)
                    .originalContent(finalContent)
                    .sourceUrl(url)
                    .source(source)
                    .category(category != null ? category : "general")
                    .language("en") // 모든 소스가 영어이므로 직접 설정
                    .publishedDate(publishedDate)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error parsing GNews article: {}", e.getMessage());
            return null;
        }
    }
    
    private boolean isCareerRelated(String text) {
        String lowerText = text.toLowerCase();
        String[] devCareerKeywords = {
            // 일반적인 커리어 키워드
            "career", "job", "employment", "hiring", "recruit", "workplace", 
            "profession", "skills", "interview", "resume", "salary", "promotion",
            "leadership", "management", "remote work", "freelance",
            
            // 개발자 관련 키워드
            "developer", "programmer", "engineer", "coding", "programming",
            "frontend", "backend", "fullstack", "devops", "software",
            
            // 기술 스택 키워드
            "javascript", "python", "java", "react", "vue", "angular", "node",
            "database", "api", "cloud", "aws", "docker", "kubernetes",
            
            // 디자인 관련 키워드
            "design", "designer", "ux", "ui", "user experience", "user interface",
            "figma", "sketch", "adobe", "product design",
            
            // 기획 관련 키워드
            "product manager", "project manager", "planning", "roadmap", 
            "strategy", "analysis", "requirements", "scrum", "agile"
        };
        
        for (String keyword : devCareerKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private LocalDateTime parsePubDate(String pubDate) {
        try {
            if (pubDate.isEmpty()) {
                return LocalDateTime.now();
            }
            return LocalDateTime.now();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private List<CareerNews> removeDuplicates(List<CareerNews> newsList) {
        Map<String, CareerNews> uniqueNews = new HashMap<>();
        
        for (CareerNews news : newsList) {
            if (!uniqueNews.containsKey(news.getSourceUrl())) {
                uniqueNews.put(news.getSourceUrl(), news);
            }
        }
        
        return new ArrayList<>(uniqueNews.values());
    }
    
    public int saveCareerNews(List<CareerNews> newsList) {
        if (newsList.isEmpty()) {
            return 0;
        }
        
        List<CareerNews> savedNews = careerNewsRepository.saveAll(newsList);
        return savedNews.size();
    }
    
    public int crawlAndSaveCareerNews(String category) {
        List<CareerNews> newsList = crawlCareerNews(category);
        return saveCareerNews(newsList);
    }
    
    public int crawlAndSaveAllSources(String category) {
        List<CareerNews> newsList = crawlAllSources(category);
        
        List<CareerNews> newsToSave = new ArrayList<>();
        for (CareerNews news : newsList) {
            if (!careerNewsRepository.existsBySourceUrl(news.getSourceUrl())) {
                newsToSave.add(news);
            }
        }
        
        if (!newsToSave.isEmpty()) {
            careerNewsRepository.saveAll(newsToSave);
        }
        
        return newsToSave.size();
    }
    
    public String testCrawlSingleUrl(String url, String sourceName) {
        return crawlFullArticleContent(url, sourceName);
    }
    
    private String crawlFullArticleContent(String articleUrl, String sourceName) {
        try {
            Document doc = Jsoup.connect(articleUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .followRedirects(true)
                .get();
                
            String content = extractContentBySource(doc, sourceName, articleUrl);
            
            if (content == null || content.trim().isEmpty()) {
                content = extractGenericContent(doc);
            }
            
            content = cleanContent(content);
            
            return content;
            
        } catch (IOException e) {
            log.error("Error crawling full content from {}: {}", articleUrl, e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("Unexpected error crawling {}: {}", articleUrl, e.getMessage());
            return "";
        }
    }

    private String extractContentBySource(Document doc, String sourceName, String url) {
        try {
            String lowerSourceName = sourceName.toLowerCase();
            
            if (lowerSourceName.contains("dev.to")) {
                return extractDevToContent(doc);
            } else if (lowerSourceName.contains("medium")) {
                return extractMediumContent(doc);
            } else if (lowerSourceName.contains("css-tricks")) {
                return extractCssTricksContent(doc);
            } else if (lowerSourceName.contains("smashing")) {
                return extractSmashingContent(doc);
            } else if (lowerSourceName.contains("alistapart")) {
                return extractAListApartContent(doc);
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Error in source-specific extraction for {}: {}", sourceName, e.getMessage());
            return null;
        }
    }

    private String extractDevToContent(Document doc) {
        String[] selectors = {
            ".crayons-article__main",
            ".article-body",
            ".content",
            "article"
        };
        
        return trySelectorsInOrder(doc, selectors, "Dev.to");
    }

    private String extractMediumContent(Document doc) {
        String[] selectors = {
            ".section-inner",
            ".post-content",
            "article",
            ".content"
        };
        
        return trySelectorsInOrder(doc, selectors, "Medium");
    }

    private String extractCssTricksContent(Document doc) {
        String[] selectors = {
            ".entry-content",
            ".post-content",
            "article .content",
            ".main-content"
        };
        
        return trySelectorsInOrder(doc, selectors, "CSS-Tricks");
    }

    private String extractSmashingContent(Document doc) {
        String[] selectors = {
            ".c-garfield-the-cat",
            ".article-content",
            ".entry-content",
            "article"
        };
        
        return trySelectorsInOrder(doc, selectors, "Smashing Magazine");
    }

    private String extractAListApartContent(Document doc) {
        String[] selectors = {
            ".entry-content",
            ".article-content",
            ".post-content",
            "article"
        };
        
        return trySelectorsInOrder(doc, selectors, "A List Apart");
    }

    private String trySelectorsInOrder(Document doc, String[] selectors, String siteName) {
        for (String selector : selectors) {
            try {
                Elements elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    String content = extractTextFromElements(elements);
                    if (isValidContent(content)) {
                        return content;
                    }
                }
            } catch (Exception e) {
                log.debug("Selector {} failed for {}: {}", selector, siteName, e.getMessage());
            }
        }
        return null;
    }

    private String extractTextFromElements(Elements elements) {
        StringBuilder content = new StringBuilder();
        
        for (Element element : elements) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                content.append(text).append("\n\n");
            }
        }
        
        return content.toString().trim();
    }

    private boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // 제한된 콘텐츠 확인
        if (isRestrictedContent(content, "")) {
            return false;
        }
        
        if (content.length() < 200) {
            return false;
        }
        
        String[] words = content.split("\\s+");
        return words.length >= 30;
    }

    private String extractGenericContent(Document doc) {
        String[] selectors = {
            "article",
            ".entry-content",
            ".post-content", 
            ".article-content",
            ".content"
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String content = elements.first().text();
                if (content.length() > 200) {
                    return content;
                }
            }
        }
        
        return doc.text();
    }

    private String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        content = content.replaceAll("<[^>]*>", "");
        content = content.replaceAll("&nbsp;", " ");
        content = content.replaceAll("&amp;", "&");
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("\n{3,}", "\n\n");
        
        return content.trim();
    }

    private CareerNews parseRSSItemWithFullContent(Element item, String sourceName, String category) {
        try {
            Element titleElement = item.select("title").first();
            if (titleElement == null) {
                return null;
            }
            String title = titleElement.text();
            
            String url = "";
            Element linkElement = item.select("link").first();
            if (linkElement != null) {
                url = linkElement.text();
                if (url.isEmpty()) {
                    url = linkElement.attr("href");
                }
            }
            
            if (title.isEmpty() || url.isEmpty()) {
                return null;
            }
            
            // RSS에서 썸네일 추출 시도
            String thumbnailUrl = "";
            
            // 다양한 RSS 썸네일 태그 확인
            Element mediaElement = item.select("media\\:thumbnail, enclosure[type^=image], image").first();
            if (mediaElement != null) {
                thumbnailUrl = mediaElement.attr("url");
                if (thumbnailUrl.isEmpty()) {
                    thumbnailUrl = mediaElement.attr("href");
                }
            }
            
            // RSS description에서 img 태그 확인
            if (thumbnailUrl.isEmpty()) {
                Element descElement = item.select("description").first();
                if (descElement != null) {
                    String descContent = descElement.text();
                    thumbnailUrl = extractImageFromDescription(descContent);
                }
            }
            
            String rssContent = "";
            Element contentElement = item.select("description").first();
            if (contentElement == null) {
                contentElement = item.select("content").first();
            }
            if (contentElement != null) {
                rssContent = cleanContent(contentElement.text());
            }
            
            String fullContent = crawlFullArticleContent(url, sourceName);
            String finalContent = (!fullContent.isEmpty() && fullContent.length() > rssContent.length()) 
                                 ? fullContent : (rssContent.isEmpty() ? title : rssContent);
            
            // 제한된 콘텐츠인지 확인
            if (!isValidArticleContent(finalContent, title)) {
                log.warn("Skipping restricted/invalid content from RSS {}: {}", sourceName, title);
                return null;
            }
            
            // 웹페이지에서 썸네일 추출 시도 (RSS에서 못 찾은 경우)
            if (thumbnailUrl.isEmpty() && !fullContent.isEmpty()) {
                thumbnailUrl = extractThumbnailFromContent(url, sourceName);
            }
            
            return CareerNews.builder()
                    .title(title)
                    .thumbnailUrl(thumbnailUrl)
                    .originalContent(finalContent)
                    .sourceUrl(url)
                    .source(sourceName)
                    .category(category != null ? category : "general")
                    .language("en")
                    .publishedDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error parsing RSS item from {}: {}", sourceName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 접근 제한된 콘텐츠인지 확인
     */
    private boolean isRestrictedContent(String content, String title) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }
        
        String lowerContent = content.toLowerCase();
        String lowerTitle = title != null ? title.toLowerCase() : "";
        
        // 비밀번호 보호 관련 키워드
        String[] passwordProtectedKeywords = {
            "password protected",
            "this content is password protected",
            "please enter your password",
            "password:",
            "enter password",
            "protected content",
            "access denied",
            "login required",
            "membership required",
            "subscription required",
            "premium content",
            "please log in",
            "sign in to continue",
            "this article is premium"
        };
        
        // 짧은 콘텐츠 (에러 페이지나 제한된 접근)
        String[] shortContentKeywords = {
            "404 not found",
            "page not found",
            "access forbidden",
            "unauthorized",
            "temporarily unavailable",
            "under maintenance",
            "coming soon",
            "no content available"
        };
        
        // 비밀번호 보호 확인
        for (String keyword : passwordProtectedKeywords) {
            if (lowerContent.contains(keyword) || lowerTitle.contains(keyword)) {
                log.warn("Detected password protected content: {}", keyword);
                return true;
            }
        }
        
        // 에러 페이지나 제한 접근 확인
        for (String keyword : shortContentKeywords) {
            if (lowerContent.contains(keyword) || lowerTitle.contains(keyword)) {
                log.warn("Detected restricted/error content: {}", keyword);
                return true;
            }
        }
        
        // 너무 짧은 콘텐츠 (100자 미만은 의미있는 기사가 아닐 가능성)
        if (content.trim().length() < 100) {
            log.warn("Content too short ({}), likely not a full article", content.length());
            return true;
        }
        
        // Career Karma 특정 제한 콘텐츠 확인
        if (lowerContent.contains("career karma is a platform designed to help job seekers") && 
            lowerContent.contains("what's next?") && content.length() < 500) {
            log.warn("Detected Career Karma restricted content template");
            return true;
        }
        
        return false;
    }
    
    /**
     * 유효한 기사 콘텐츠인지 확인
     */
    private boolean isValidArticleContent(String content, String title) {
        if (isRestrictedContent(content, title)) {
            return false;
        }
        
        // 최소 콘텐츠 길이 확인
        if (content.trim().length() < 200) {
            return false;
        }
        
        // 단어 수 확인 (최소 30단어)
        String[] words = content.split("\\s+");
        if (words.length < 30) {
            return false;
        }
        
        return true;
    }
    private String extractThumbnailFromContent(String articleUrl, String sourceName) {
        try {
            Document doc = Jsoup.connect(articleUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .followRedirects(true)
                .get();
                
            String thumbnailUrl = "";
            
            // 1. Open Graph 이미지 (가장 일반적)
            Element ogImage = doc.select("meta[property=og:image]").first();
            if (ogImage != null) {
                thumbnailUrl = ogImage.attr("content");
            }
            
            // 2. Twitter Card 이미지
            if (thumbnailUrl.isEmpty()) {
                Element twitterImage = doc.select("meta[name=twitter:image]").first();
                if (twitterImage != null) {
                    thumbnailUrl = twitterImage.attr("content");
                }
            }
            
            // 3. 기사의 첫 번째 이미지
            if (thumbnailUrl.isEmpty()) {
                thumbnailUrl = extractFirstArticleImage(doc, sourceName);
            }
            
            // 4. 일반적인 이미지 태그에서 추출
            if (thumbnailUrl.isEmpty()) {
                Element firstImg = doc.select("img[src]").first();
                if (firstImg != null) {
                    String imgSrc = firstImg.attr("src");
                    if (isValidImageUrl(imgSrc)) {
                        thumbnailUrl = makeAbsoluteUrl(imgSrc, articleUrl);
                    }
                }
            }
            
            log.debug("Extracted thumbnail for {}: {}", sourceName, thumbnailUrl);
            return thumbnailUrl;
            
        } catch (Exception e) {
            log.warn("Error extracting thumbnail from {}: {}", articleUrl, e.getMessage());
            return "";
        }
    }
    
    /**
     * 소스별 첫 번째 기사 이미지 추출
     */
    private String extractFirstArticleImage(Document doc, String sourceName) {
        String lowerSourceName = sourceName.toLowerCase();
        
        if (lowerSourceName.contains("dev.to")) {
            return extractDevToThumbnail(doc);
        } else if (lowerSourceName.contains("medium")) {
            return extractMediumThumbnail(doc);
        } else if (lowerSourceName.contains("css-tricks")) {
            return extractCssTricksThumbnail(doc);
        } else if (lowerSourceName.contains("smashing")) {
            return extractSmashingThumbnail(doc);
        } else if (lowerSourceName.contains("alistapart")) {
            return extractAListApartThumbnail(doc);
        }
        
        return extractGenericThumbnail(doc);
    }
    
    private String extractDevToThumbnail(Document doc) {
        String[] selectors = {
            ".crayons-article__cover img",
            ".article-cover img",
            ".featured-image img",
            "article img"
        };
        
        return tryThumbnailSelectors(doc, selectors);
    }
    
    private String extractMediumThumbnail(Document doc) {
        String[] selectors = {
            ".graf-image",
            ".section-inner img",
            "figure img",
            "article img"
        };
        
        return tryThumbnailSelectors(doc, selectors);
    }
    
    private String extractCssTricksThumbnail(Document doc) {
        String[] selectors = {
            ".featured-image img",
            ".entry-header img",
            ".post-thumbnail img",
            "article img"
        };
        
        return tryThumbnailSelectors(doc, selectors);
    }
    
    private String extractSmashingThumbnail(Document doc) {
        String[] selectors = {
            ".c-featured-panel__image img",
            ".article-header img",
            ".featured-image img",
            "article img"
        };
        
        return tryThumbnailSelectors(doc, selectors);
    }
    
    private String extractAListApartThumbnail(Document doc) {
        String[] selectors = {
            ".entry-header img",
            ".featured-image img",
            ".post-image img",
            "article img"
        };
        
        return tryThumbnailSelectors(doc, selectors);
    }
    
    private String extractGenericThumbnail(Document doc) {
        String[] selectors = {
            "article img",
            ".post-content img",
            ".entry-content img",
            ".content img",
            ".main img"
        };
        
        return tryThumbnailSelectors(doc, selectors);
    }
    
    private String tryThumbnailSelectors(Document doc, String[] selectors) {
        for (String selector : selectors) {
            Elements imgs = doc.select(selector);
            for (Element img : imgs) {
                String src = img.attr("src");
                if (isValidImageUrl(src)) {
                    return src;
                }
            }
        }
        return "";
    }
    
    /**
     * RSS description에서 이미지 추출
     */
    private String extractImageFromDescription(String description) {
        try {
            Document doc = Jsoup.parse(description);
            Element img = doc.select("img").first();
            if (img != null) {
                String src = img.attr("src");
                if (isValidImageUrl(src)) {
                    return src;
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting image from description: {}", e.getMessage());
        }
        return "";
    }
    
    /**
     * 유효한 이미지 URL인지 확인
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // 너무 작은 이미지 제외 (아이콘 등)
        if (url.contains("icon") || url.contains("logo") || url.contains("avatar")) {
            return false;
        }
        
        // 유효한 이미지 확장자
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || 
               lowerUrl.contains(".png") || lowerUrl.contains(".webp") ||
               lowerUrl.contains(".gif");
    }
    
    /**
     * 상대 URL을 절대 URL로 변환
     */
    private String makeAbsoluteUrl(String url, String baseUrl) {
        try {
            if (url.startsWith("http")) {
                return url;
            }
            
            if (url.startsWith("//")) {
                return "https:" + url;
            }
            
            if (url.startsWith("/")) {
                String domain = baseUrl.split("/")[0] + "//" + baseUrl.split("/")[2];
                return domain + url;
            }
            
            return baseUrl + "/" + url;
        } catch (Exception e) {
            return url;
        }
    }
    
    private String detectLanguage(String sourceName, String title, String content) {
        // 모든 소스가 영어이므로 항상 "en" 반환
        return "en";
    }
    
    private boolean containsSignificantKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        int koreanChars = 0;
        int totalChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                totalChars++;
                if (c >= 0xAC00 && c <= 0xD7AF) {
                    koreanChars++;
                }
            }
        }
        
        return totalChars > 0 && (double) koreanChars / totalChars > 0.3;
    }
}