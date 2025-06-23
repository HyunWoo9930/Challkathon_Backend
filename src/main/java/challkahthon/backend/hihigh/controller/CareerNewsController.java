package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.service.CareerNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/career-news")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "커리어 뉴스", description = "커리어 뉴스 관련 API")
public class CareerNewsController {

    private final CareerNewsService careerNewsService;

    @Operation(
        summary = "카테고리별 최신 뉴스 조회",
        description = "지정된 카테고리의 최신 뉴스를 조회합니다. 카테고리가 지정되지 않으면 모든 카테고리의 최신 뉴스를 반환합니다."
    )
    @GetMapping("/latest")
    public ResponseEntity<List<CareerNewsDto>> getLatestNews(
            @Parameter(description = "뉴스 카테고리 (frontend, backend, design, planning, devops)")
            @RequestParam(required = false) String category) {
        List<CareerNews> newsList = careerNewsService.getLatestNewsByCategory(category);
        List<CareerNewsDto> newsListDto = newsList.stream()
                .map(CareerNewsDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(newsListDto);
    }

    @Operation(
        summary = "뉴스 상세 조회",
        description = "지정된 ID의 뉴스 상세 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getNewsById(
            @Parameter(description = "뉴스 ID")
            @PathVariable Long id) {
        CareerNews news = careerNewsService.getNewsById(id);
        if (news == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(CareerNewsDto.fromEntityWithContent(news));
    }

    @Operation(
        summary = "키워드로 뉴스 검색",
        description = "지정된 키워드가 포함된 뉴스를 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<List<CareerNewsDto>> searchNews(
            @Parameter(description = "검색 키워드")
            @RequestParam String keyword) {
        List<CareerNews> newsList = careerNewsService.searchNewsByKeyword(keyword);
        List<CareerNewsDto> newsListDto = newsList.stream()
                .map(CareerNewsDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(newsListDto);
    }

    @Operation(
        summary = "뉴스 크롤링 및 처리 실행",
        description = "지정된 카테고리의 뉴스를 크롤링하고 처리합니다. 관리자만 접근 가능합니다."
    )
    @PostMapping("/crawl")
    public ResponseEntity<?> crawlNews(
            @Parameter(description = "뉴스 카테고리 (frontend, backend, design, planning, devops)")
            @RequestParam(required = false) String category) {
        int count = careerNewsService.crawlAndProcessNews(category);
        return ResponseEntity.ok(Map.of(
                "message", "뉴스 크롤링 및 번역 처리가 완료되었습니다. (제목/요약 즉시, 본문 비동기)",
                "count", count
        ));
    }

    @Operation(
        summary = "미처리된 뉴스 처리",
        description = "번역이나 요약이 되지 않은 뉴스를 처리합니다. 관리자만 접근 가능합니다."
    )
    @PostMapping("/process-unprocessed")
    public ResponseEntity<?> processUnprocessedNews() {
        int count = careerNewsService.processAllUnprocessedNews();
        return ResponseEntity.ok(Map.of(
                "message", "미처리된 뉴스 번역 및 요약 처리가 완료되었습니다.",
                "count", count
        ));
    }

    @Operation(
        summary = "사용 가능한 카테고리 조회",
        description = "사용 가능한 모든 뉴스 카테고리를 조회합니다."
    )
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = careerNewsService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
        summary = "뉴스 내용 길이 확인",
        description = "저장된 뉴스의 실제 내용 길이를 확인합니다."
    )
    @GetMapping("/{id}/content-info")
    public ResponseEntity<Map<String, Object>> getNewsContentInfo(
            @Parameter(description = "뉴스 ID")
            @PathVariable Long id) {
        CareerNews news = careerNewsService.getNewsById(id);
        if (news == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("id", news.getId());
        info.put("title", news.getTitle());
        info.put("titleLength", news.getTitle() != null ? news.getTitle().length() : 0);
        info.put("thumbnailUrl", news.getThumbnailUrl());
        info.put("source", news.getSource());
        info.put("originalContentLength", news.getOriginalContent() != null ? news.getOriginalContent().length() : 0);
        info.put("translatedContentLength", news.getTranslatedContent() != null ? news.getTranslatedContent().length() : 0);
        info.put("summaryLength", news.getSummary() != null ? news.getSummary().length() : 0);
        info.put("translationStatus", "active");
        
        // 내용의 첫 200자만 미리보기로 제공
        if (news.getOriginalContent() != null) {
            info.put("originalContentPreview", news.getOriginalContent().substring(0, 
                Math.min(200, news.getOriginalContent().length())) + "...");
        }
        
        if (news.getTranslatedContent() != null) {
            info.put("translatedContentPreview", news.getTranslatedContent().substring(0, 
                Math.min(200, news.getTranslatedContent().length())) + "...");
        }
        
        if (news.getSummary() != null) {
            info.put("summaryPreview", news.getSummary().substring(0, 
                Math.min(200, news.getSummary().length())) + "...");
        }
        
        info.put("translationNote", "제목과 요약은 즉시 번역, 본문은 비동기 번역됩니다.");
        
        return ResponseEntity.ok(info);
    }

    @Operation(
        summary = "뉴스 원본 내용 전체 조회",
        description = "저장된 뉴스의 원본 내용을 전체 조회합니다."
    )
    @GetMapping("/{id}/full-content")
    public ResponseEntity<Map<String, Object>> getNewsFullContent(
            @Parameter(description = "뉴스 ID")
            @PathVariable Long id) {
        CareerNews news = careerNewsService.getNewsById(id);
        if (news == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> content = new HashMap<>();
        content.put("id", news.getId());
        content.put("title", news.getTitle());
        content.put("thumbnailUrl", news.getThumbnailUrl());
        content.put("source", news.getSource());
        content.put("sourceUrl", news.getSourceUrl());
        content.put("originalContent", news.getOriginalContent());
        content.put("translatedContent", news.getTranslatedContent());
        content.put("summary", news.getSummary());
        
        // 표시할 콘텐츠 결정 (번역된 내용이 있으면 번역본, 없으면 원본)
        String displayContent = (news.getTranslatedContent() != null && !news.getTranslatedContent().trim().isEmpty()) 
                ? news.getTranslatedContent() : news.getOriginalContent();
        content.put("displayContent", displayContent);
        
        content.put("translationNote", "제목과 요약은 즉시 번역됩니다. 본문 번역은 백그라운드에서 진행됩니다.");
        content.put("contentLanguage", (news.getTranslatedContent() != null && !news.getTranslatedContent().trim().isEmpty()) ? "ko" : "en");
        
        return ResponseEntity.ok(content);
    }

    @Operation(
        summary = "번역 상태 확인",
        description = "뉴스의 번역 상태와 표시될 콘텐츠를 확인합니다."
    )
    @GetMapping("/{id}/translation-status")
    public ResponseEntity<Map<String, Object>> getTranslationStatus(
            @Parameter(description = "뉴스 ID")
            @PathVariable Long id) {
        CareerNews news = careerNewsService.getNewsById(id);
        if (news == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("id", news.getId());
        status.put("title", news.getTitle());
        status.put("language", news.getLanguage());
        
        // 번역 상태 확인
        boolean hasTranslatedContent = news.getTranslatedContent() != null && !news.getTranslatedContent().trim().isEmpty();
        boolean hasTranslatedTitle = news.getLanguage().equals("en") && containsKorean(news.getTitle());
        boolean hasTranslatedSummary = news.getSummary() != null && containsKorean(news.getSummary());
        
        status.put("hasTranslatedContent", hasTranslatedContent);
        status.put("hasTranslatedTitle", hasTranslatedTitle);
        status.put("hasTranslatedSummary", hasTranslatedSummary);
        
        // 표시될 콘텐츠 정보
        String displayContent = hasTranslatedContent ? news.getTranslatedContent() : news.getOriginalContent();
        status.put("displayContentLanguage", hasTranslatedContent ? "ko" : "en");
        status.put("displayContentLength", displayContent != null ? displayContent.length() : 0);
        
        status.put("translationProgress", calculateTranslationProgress(hasTranslatedTitle, hasTranslatedSummary, hasTranslatedContent));
        
        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "뉴스 소스별 통계 조회",
        description = "각 뉴스 소스별 기사 수를 조회합니다."
    )
    @GetMapping("/stats/sources")
    public ResponseEntity<Map<String, Long>> getSourceStats() {
        Map<String, Long> stats = careerNewsService.getSourceStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @Operation(
        summary = "언어별 통계 조회",
        description = "언어별 기사 수를 조회합니다."
    )
    @GetMapping("/stats/languages")
    public ResponseEntity<Map<String, Long>> getLanguageStats() {
        Map<String, Long> stats = careerNewsService.getLanguageStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @Operation(
        summary = "카테고리별 통계 조회",
        description = "각 카테고리별 기사 수를 조회합니다."
    )
    @GetMapping("/stats/categories")
    public ResponseEntity<Map<String, Long>> getCategoryStats() {
        Map<String, Long> stats = careerNewsService.getCategoryStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @Operation(
        summary = "번역 서비스 테스트 (제목/요약 우선)",
        description = "제목과 요약을 우선적으로 번역하는 서비스를 테스트합니다."
    )
    @GetMapping("/test-translation")
    public ResponseEntity<Map<String, String>> testTranslation(
            @Parameter(description = "테스트할 영어 텍스트")
            @RequestParam(defaultValue = "Hello, this is a career news translation test!") String text) {
        
        Map<String, String> result = new HashMap<>();
        result.put("originalText", text);
        
        try {
            String translatedText = careerNewsService.testTranslation(text);
            result.put("translatedText", translatedText);
            result.put("status", "success");
            result.put("method", text.length() <= 200 ? "title_translation" : "summary_translation");
            result.put("originalLength", String.valueOf(text.length()));
            result.put("translatedLength", String.valueOf(translatedText.length()));
        } catch (Exception e) {
            result.put("translatedText", "Translation failed: " + e.getMessage());
            result.put("status", "error");
        }
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "크롤링 필터링 테스트",
        description = "특정 텍스트가 제한된 콘텐츠인지 확인합니다."
    )
    @GetMapping("/test-filter")
    public ResponseEntity<Map<String, Object>> testContentFilter(
            @Parameter(description = "테스트할 텍스트")
            @RequestParam String content,
            @Parameter(description = "제목 (선택사항)")
            @RequestParam(required = false, defaultValue = "") String title) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("title", title);
        result.put("contentLength", content.length());
        
        // 여기서는 WebCrawlerService의 private 메서드를 테스트할 수 없으므로
        // 간단한 필터링 로직을 구현
        boolean isRestricted = content.toLowerCase().contains("password protected") ||
                              content.toLowerCase().contains("please enter your password") ||
                              content.toLowerCase().contains("this content is password protected") ||
                              content.length() < 100;
        
        result.put("isRestricted", isRestricted);
        result.put("isValid", !isRestricted && content.length() >= 200);
        result.put("reason", isRestricted ? "Content appears to be password protected or too short" : "Content appears valid");
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "특정 URL 크롤링 테스트",
        description = "특정 URL의 전체 본문을 크롤링해서 결과를 확인합니다."
    )
    @GetMapping("/test-crawl")
    public ResponseEntity<Map<String, Object>> testCrawlUrl(
            @Parameter(description = "크롤링할 URL")
            @RequestParam String url,
            @Parameter(description = "소스 이름")
            @RequestParam(defaultValue = "Dev.to") String sourceName) {
        
        Map<String, Object> result = careerNewsService.testCrawlUrl(url, sourceName);
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "현재 설정된 뉴스 소스 목록 조회",
        description = "현재 크롤링하고 있는 모든 뉴스 소스의 목록과 정보를 조회합니다."
    )
    @GetMapping("/sources/list")
    public ResponseEntity<Map<String, Object>> getNewsSources() {
        Map<String, Object> sources = careerNewsService.getConfiguredSources();
        return ResponseEntity.ok(sources);
    }
    
    @Operation(
        summary = "서버 상태 확인",
        description = "서버가 정상적으로 작동하는지 확인합니다."
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("message", "서버가 정상적으로 작동 중입니다.");
        return ResponseEntity.ok(health);
    }
    
    // Helper methods
    private int calculateTranslationProgress(boolean hasTitle, boolean hasSummary, boolean hasContent) {
        int progress = 0;
        if (hasTitle) progress += 33;
        if (hasSummary) progress += 33;
        if (hasContent) progress += 34;
        return progress;
    }
    
    private boolean containsKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7AF) {
                return true;
            }
        }
        return false;
    }
}
