package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.dto.response.MainPageResponseDto;
import challkahthon.backend.hihigh.service.MainPageService;
import challkahthon.backend.hihigh.service.CareerNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "메인 페이지", description = "메인 페이지 관련 API")
public class MainPageController {

	private final MainPageService mainPageService;
	private final CareerNewsService careerNewsService;

	@Operation(
		summary = "메인 페이지 뉴스 조회",
		description = "메인 페이지에 표시할 사용자 맞춤 뉴스와 카테고리별 뉴스 목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<?> getMainPageNews(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		
		try {
			// 기존 메인페이지 응답 (일반 뉴스)
			MainPageResponseDto response = mainPageService.getMainPageNews();
			response.setName(username);
			
			// 사용자 맞춤 뉴스 추가
			var personalizedNews = careerNewsService.getPersonalizedNews(username, null, 5);
			var personalizedNewsDto = personalizedNews.stream()
				.map(news -> challkahthon.backend.hihigh.dto.CareerNewsDto.fromEntity(news))
				.toList();
			
			// 응답에 맞춤 뉴스 정보 추가 (기존 구조 유지를 위해 별도 필드로)
			response.setPersonalizedNews(personalizedNewsDto);
			response.setHasPersonalizedNews(!personalizedNewsDto.isEmpty());
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			// 오류 발생 시 기본 뉴스라도 반환
			MainPageResponseDto response = mainPageService.getMainPageNews();
			response.setName(username);
			return ResponseEntity.ok(response);
		}
	}

	@Operation(
		summary = "관심사 업데이트 후 즉시 크롤링",
		description = "사용자가 관심사를 업데이트한 후 맞춤 뉴스를 즉시 크롤링합니다."
	)
	@GetMapping("/refresh-personalized")
	public ResponseEntity<?> refreshPersonalizedNews(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		
		try {
			// 즉시 크롤링 트리거
			careerNewsService.triggerPersonalizedCrawling(username);
			
			return ResponseEntity.ok("맞춤 뉴스 업데이트가 시작되었습니다. 잠시 후 새로운 뉴스를 확인해주세요.");
			
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("맞춤 뉴스 업데이트 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
}
