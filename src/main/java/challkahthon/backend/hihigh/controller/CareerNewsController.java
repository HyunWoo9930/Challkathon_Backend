package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.service.CareerNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

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
		summary = "뉴스 크롤링 및 처리 실행(관리자)",
		description = "지정된 카테고리의 뉴스를 크롤링하고 처리합니다. 관리자만 접근 가능합니다."
	)
	@PostMapping("/crawl")
	public ResponseEntity<?> crawlNews(
		@Parameter(description = "뉴스 카테고리 (frontend, backend, design, planning, devops)")
		@RequestParam(required = false) String category,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		int count = careerNewsService.crawlAndProcessNews(category);
		return ResponseEntity.ok(Map.of(
			"message", "뉴스 크롤링 및 번역 처리가 완료되었습니다. (제목/요약 즉시, 본문 비동기)",
			"count", count
		));
	}

	@Operation(
		summary = "미처리된 뉴스 처리(관리자)",
		description = "번역이나 요약이 되지 않은 뉴스를 처리합니다. 관리자만 접근 가능합니다."
	)
	@PostMapping("/process-unprocessed")
	public ResponseEntity<?> processUnprocessedNews(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

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

	// ===== AI 분석 관련 엔드포인트들 =====

	@Operation(
		summary = "AI 분석 미처리 뉴스 일괄 분석",
		description = "AI 분석이 되지 않은 뉴스들을 일괄적으로 분석합니다."
	)
	@PostMapping("/ai/analyze-unprocessed")
	public ResponseEntity<Map<String, Object>> analyzeUnprocessedNews(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body(Map.of("error", "인증되지 않은 사용자입니다."));
		}

		int count = careerNewsService.analyzeUnanalyzedNews();
		Map<String, Object> result = new HashMap<>();
		result.put("message", "AI 분석이 완료되었습니다.");
		result.put("analyzedCount", count);
		result.put("timestamp", LocalDateTime.now());
		return ResponseEntity.ok(result);
	}

	@Operation(
		summary = "카테고리 자동 재분류",
		description = "AI를 사용해서 모든 뉴스의 카테고리를 재분류합니다."
	)
	@PostMapping("/ai/reclassify-categories")
	public ResponseEntity<Map<String, Object>> reclassifyCategories(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body(Map.of("error", "인증되지 않은 사용자입니다."));
		}

		int count = careerNewsService.autoReclassifyCategories();
		Map<String, Object> result = new HashMap<>();
		result.put("message", "카테고리 재분류가 완료되었습니다.");
		result.put("reclassifiedCount", count);
		result.put("timestamp", LocalDateTime.now());
		return ResponseEntity.ok(result);
	}

	@Operation(
		summary = "키워드 재추출",
		description = "AI를 사용해서 모든 뉴스의 키워드를 재추출합니다."
	)
	@PostMapping("/ai/reextract-keywords")
	public ResponseEntity<Map<String, Object>> reextractKeywords(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body(Map.of("error", "인증되지 않은 사용자입니다."));
		}

		int count = careerNewsService.reextractKeywords();
		Map<String, Object> result = new HashMap<>();
		result.put("message", "키워드 재추출이 완료되었습니다.");
		result.put("updatedCount", count);
		result.put("timestamp", LocalDateTime.now());
		return ResponseEntity.ok(result);
	}

	@Operation(
		summary = "AI 분석 통계 조회",
		description = "AI 분석 결과에 대한 통계를 조회합니다."
	)
	@GetMapping("/ai/statistics")
	public ResponseEntity<Map<String, Object>> getAIStatistics() {
		Map<String, Object> stats = careerNewsService.getAIAnalysisStatistics();
		return ResponseEntity.ok(stats);
	}

	@Operation(
		summary = "고관련성 뉴스 조회",
		description = "AI가 판단한 관련성 점수가 높은 뉴스들을 조회합니다."
	)
	@GetMapping("/ai/high-relevance")
	public ResponseEntity<List<CareerNewsDto>> getHighRelevanceNews(
		@Parameter(description = "최소 관련성 점수 (0.0 ~ 1.0)")
		@RequestParam(defaultValue = "0.7") double minScore) {
		List<CareerNews> newsList = careerNewsService.getHighRelevanceNews(minScore);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "카테고리 일치 뉴스 조회",
		description = "AI가 카테고리가 정확하다고 판단한 뉴스들을 조회합니다."
	)
	@GetMapping("/ai/category-matched")
	public ResponseEntity<List<CareerNewsDto>> getCategoryMatchedNews(
		@Parameter(description = "카테고리")
		@RequestParam String category) {
		List<CareerNews> newsList = careerNewsService.getCategoryMatchedNews(category);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "향상된 키워드 검색",
		description = "AI가 추출한 키워드를 포함한 향상된 검색을 수행합니다."
	)
	@GetMapping("/ai/search-enhanced")
	public ResponseEntity<List<CareerNewsDto>> searchEnhanced(
		@Parameter(description = "검색 키워드")
		@RequestParam String keyword) {
		List<CareerNews> newsList = careerNewsService.searchByKeywordEnhanced(keyword);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "AI 분석 테스트",
		description = "특정 텍스트에 대해 AI 분석을 테스트합니다."
	)
	@PostMapping("/ai/test-analysis")
	public ResponseEntity<Map<String, Object>> testAIAnalysis(
		@Parameter(description = "기사 제목")
		@RequestParam String title,
		@Parameter(description = "기사 내용")
		@RequestParam String content,
		@Parameter(description = "타겟 카테고리")
		@RequestParam(defaultValue = "general") String category,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body(Map.of("error", "인증되지 않은 사용자입니다."));
		}

		Map<String, Object> result = careerNewsService.testAIAnalysis(title, content, category);
		return ResponseEntity.ok(result);
	}

	@Operation(
		summary = "뉴스 AI 분석 정보 조회",
		description = "특정 뉴스의 AI 분석 결과를 조회합니다."
	)
	@GetMapping("/{id}/ai-analysis")
	public ResponseEntity<Map<String, Object>> getNewsAIAnalysis(
		@Parameter(description = "뉴스 ID")
		@PathVariable Long id) {
		CareerNews news = careerNewsService.getNewsById(id);
		if (news == null) {
			return ResponseEntity.notFound().build();
		}

		Map<String, Object> analysis = new HashMap<>();
		analysis.put("id", news.getId());
		analysis.put("title", news.getTitle());
		analysis.put("category", news.getCategory());
		analysis.put("isAiAnalyzed", news.getIsAiAnalyzed());
		analysis.put("isRelevant", news.getIsRelevant());
		analysis.put("categoryMatch", news.getCategoryMatch());
		analysis.put("relevanceScore", news.getRelevanceScore());
		analysis.put("suggestedCategory", news.getSuggestedCategory());
		analysis.put("keywords", news.getKeywords());
		analysis.put("analysisReason", news.getAnalysisReason());

		// 분석 결과 해석
		String interpretation = "";
		if (news.getIsAiAnalyzed() != null && news.getIsAiAnalyzed()) {
			if (news.getIsRelevant() != null && news.getIsRelevant()) {
				if (news.getCategoryMatch() != null && news.getCategoryMatch()) {
					interpretation = "적절한 카테고리의 관련성 높은 기사";
				} else {
					interpretation = "관련성은 있지만 카테고리가 부정확할 수 있는 기사";
				}
			} else {
				interpretation = "개발/커리어와 관련성이 낮은 기사";
			}
		} else {
			interpretation = "아직 AI 분석이 되지 않은 기사";
		}
		analysis.put("interpretation", interpretation);

		return ResponseEntity.ok(analysis);
	}

	// Helper methods
	private int calculateTranslationProgress(boolean hasTitle, boolean hasSummary, boolean hasContent) {
		int progress = 0;
		if (hasTitle)
			progress += 33;
		if (hasSummary)
			progress += 33;
		if (hasContent)
			progress += 34;
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
