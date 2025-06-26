package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.dto.request.PersonalizedNewsRequestDto;
import challkahthon.backend.hihigh.domain.dto.response.PersonalizedNewsResponseDto;
import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.service.CareerNewsService;
import challkahthon.backend.hihigh.service.CareerNewsService.PersonalizedNewsStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/career-news")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "커리어 뉴스", description = "사용자 맞춤 커리어 뉴스 API")
public class CareerNewsController {

	private final CareerNewsService careerNewsService;

	@Operation(
		summary = "사용자 맞춤 뉴스 조회",
		description = "사용자의 관심사에 기반한 맞춤 뉴스와 일반 뉴스를 함께 조회합니다."
	)
	@PostMapping("/personalized")
	public ResponseEntity<?> getPersonalizedNews(
		@RequestBody(required = false) PersonalizedNewsRequestDto request,
		Authentication authentication) {
		
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		
		if (request == null) {
			request = PersonalizedNewsRequestDto.builder().build();
		}

		try {
			// 사용자 맞춤 뉴스 조회
			List<CareerNews> personalizedNews;
			if (request.getHighRelevanceOnly()) {
				personalizedNews = careerNewsService.getHighRelevanceNews(
					username, request.getMinRelevanceScore(), request.getSize());
			} else if (request.getInterest() != null && !request.getInterest().trim().isEmpty()) {
				personalizedNews = careerNewsService.getNewsByUserInterest(
					username, request.getInterest(), request.getSize());
			} else {
				personalizedNews = careerNewsService.getPersonalizedNews(
					username, request.getCategory(), request.getSize());
			}

			// 일반 뉴스 조회 (전체 사용자 대상)
			List<CareerNews> generalNews = careerNewsService.getLatestNewsByCategory(
				request.getCategory());

			// DTO 변환
			List<CareerNewsDto> personalizedNewsDto = personalizedNews.stream()
				.map(CareerNewsDto::fromEntity)
				.collect(Collectors.toList());

			List<CareerNewsDto> generalNewsDto = generalNews.stream()
				.map(CareerNewsDto::fromEntity)
				.collect(Collectors.toList());

			// 통계 정보 조회
			PersonalizedNewsStats stats = careerNewsService.getPersonalizedNewsStats(username);

			PersonalizedNewsResponseDto response = PersonalizedNewsResponseDto.of(
				personalizedNewsDto, generalNewsDto, stats);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body("뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	@Operation(
		summary = "카테고리별 최신 뉴스 조회 (기존 API 호환성)",
		description = "지정된 카테고리의 최신 뉴스를 조회합니다. 전체 사용자 대상 뉴스만 반환합니다."
	)
	@GetMapping("/latest")
	public ResponseEntity<List<CareerNewsDto>> getLatestNews(
		@Parameter(description = "뉴스 카테고리")
		@RequestParam(required = false) String category) {
		
		List<CareerNews> newsList = careerNewsService.getLatestNewsByCategory(category);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "맞춤 뉴스 통계 조회",
		description = "사용자의 맞춤 뉴스 통계 정보를 조회합니다."
	)
	@GetMapping("/stats")
	public ResponseEntity<?> getPersonalizedNewsStats(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		try {
			PersonalizedNewsStats stats = careerNewsService.getPersonalizedNewsStats(username);
			return ResponseEntity.ok(stats);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("통계 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	@Operation(
		summary = "맞춤 뉴스 크롤링 즉시 실행",
		description = "사용자의 관심사 기반 맞춤 뉴스를 즉시 크롤링합니다."
	)
	@PostMapping("/crawl")
	public ResponseEntity<?> triggerPersonalizedCrawling(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		try {
			careerNewsService.triggerPersonalizedCrawling(username);
			return ResponseEntity.ok("맞춤 뉴스 크롤링이 시작되었습니다. 잠시 후 새로운 뉴스를 확인해주세요.");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("크롤링 실행 중 오류가 발생했습니다: " + e.getMessage());
		}
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
		summary = "고관련성 뉴스만 조회",
		description = "사용자 관심사와 높은 관련성을 보이는 뉴스만 조회합니다."
	)
	@GetMapping("/high-relevance")
	public ResponseEntity<?> getHighRelevanceNews(
		@Parameter(description = "최소 관련성 점수 (0.0-1.0)")
		@RequestParam(defaultValue = "0.7") Double minScore,
		@Parameter(description = "조회할 뉴스 개수")
		@RequestParam(defaultValue = "10") Integer size,
		Authentication authentication) {
		
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		try {
			List<CareerNews> newsList = careerNewsService.getHighRelevanceNews(username, minScore, size);
			List<CareerNewsDto> newsListDto = newsList.stream()
				.map(CareerNewsDto::fromEntity)
				.collect(Collectors.toList());
			return ResponseEntity.ok(newsListDto);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("고관련성 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
}
