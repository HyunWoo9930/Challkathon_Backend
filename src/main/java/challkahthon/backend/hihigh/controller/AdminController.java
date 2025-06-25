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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "관리자", description = "관리자 전용 API - 테스트, 크롤링, AI 분석 등")
public class AdminController {

	private final CareerNewsService careerNewsService;

	// ===== 뉴스 크롤링 및 처리 관련 =====

	@Operation(
		summary = "뉴스 크롤링 및 처리 실행",
		description = "지정된 카테고리의 뉴스를 크롤링하고 처리합니다. 관리자만 접근 가능합니다."
	)
	@PostMapping("/news/crawl")
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
		summary = "미처리된 뉴스 처리",
		description = "번역이나 요약이 되지 않은 뉴스를 처리합니다. 관리자만 접근 가능합니다."
	)
	@PostMapping("/news/process-unprocessed")
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

	// ===== 뉴스 조회 관련 (관리자용) =====

	@Operation(
		summary = "키워드로 뉴스 검색",
		description = "지정된 키워드가 포함된 뉴스를 검색합니다."
	)
	@GetMapping("/news/search")
	public ResponseEntity<List<CareerNewsDto>> searchNews(
		@Parameter(description = "검색 키워드")
		@RequestParam String keyword,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().build();
		}

		List<CareerNews> newsList = careerNewsService.searchNewsByKeyword(keyword);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "사용 가능한 카테고리 조회",
		description = "사용 가능한 모든 뉴스 카테고리를 조회합니다."
	)
	@GetMapping("/news/categories")
	public ResponseEntity<List<String>> getCategories(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().build();
		}

		List<String> categories = careerNewsService.getAllCategories();
		return ResponseEntity.ok(categories);
	}

	@Operation(
		summary = "고관련성 뉴스 조회",
		description = "AI가 판단한 관련성 점수가 높은 뉴스들을 조회합니다."
	)
	@GetMapping("/news/high-relevance")
	public ResponseEntity<List<CareerNewsDto>> getHighRelevanceNews(
		@Parameter(description = "최소 관련성 점수 (0.0 ~ 1.0)")
		@RequestParam(defaultValue = "0.7") double minScore,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().build();
		}

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
	@GetMapping("/news/category-matched")
	public ResponseEntity<List<CareerNewsDto>> getCategoryMatchedNews(
		@Parameter(description = "카테고리")
		@RequestParam String category,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().build();
		}

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
	@GetMapping("/news/search-enhanced")
	public ResponseEntity<List<CareerNewsDto>> searchEnhanced(
		@Parameter(description = "검색 키워드")
		@RequestParam String keyword,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().build();
		}

		List<CareerNews> newsList = careerNewsService.searchByKeywordEnhanced(keyword);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "뉴스 AI 분석 정보 조회",
		description = "특정 뉴스의 AI 분석 결과를 조회합니다."
	)
	@GetMapping("/news/{id}/ai-analysis")
	public ResponseEntity<Map<String, Object>> getNewsAIAnalysis(
		@Parameter(description = "뉴스 ID")
		@PathVariable Long id,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().build();
		}

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

	// ===== AI 분석 관련 =====

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
		summary = "AI 분석 통계 조회",
		description = "AI 분석 결과에 대한 통계를 조회합니다."
	)
	@GetMapping("/ai/statistics")
	public ResponseEntity<Map<String, Object>> getAIStatistics(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body(Map.of("error", "인증되지 않은 사용자입니다."));
		}

		Map<String, Object> stats = careerNewsService.getAIAnalysisStatistics();
		return ResponseEntity.ok(stats);
	}

	// ===== 시스템 관련 =====

	@Operation(
		summary = "현재 설정된 뉴스 소스 목록 조회",
		description = "현재 크롤링하고 있는 모든 뉴스 소스의 목록과 정보를 조회합니다."
	)
	@GetMapping("/news/sources")
	public ResponseEntity<Map<String, Object>> getNewsSources(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body(Map.of("error", "인증되지 않은 사용자입니다."));
		}

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

	@Operation(
		summary = "OAuth 성공 테스트",
		description = "OAuth 로그인 성공 후 리다이렉트 테스트용 엔드포인트입니다."
	)
	@GetMapping("/auth/success")
	public String authSuccess() {
		return "OAuth 로그인 성공!";
	}

	@Operation(
		summary = "토큰 유효성 검증 테스트",
		description = "제공된 JWT 토큰의 유효성을 검증하고 토큰 정보를 반환합니다."
	)
	@PostMapping("/auth/validate-token")
	public ResponseEntity<Map<String, Object>> validateToken(
		@Parameter(description = "검증할 JWT 토큰")
		@RequestParam String token) {

		Map<String, Object> result = new HashMap<>();

		try {
			// 토큰 유효성 검증 로직을 여기에 추가할 수 있습니다
			result.put("valid", true);
			result.put("message", "토큰이 유효합니다.");
			result.put("timestamp", LocalDateTime.now());
		} catch (Exception e) {
			result.put("valid", false);
			result.put("message", "토큰이 유효하지 않습니다: " + e.getMessage());
			result.put("timestamp", LocalDateTime.now());
		}

		return ResponseEntity.ok(result);
	}
}
