package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.dto.response.MainPageResponseDto;
import challkahthon.backend.hihigh.service.MainPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "메인 페이지", description = "메인 페이지 관련 API")
public class MainPageController {

	private final MainPageService mainPageService;

	@Operation(
		summary = "메인 페이지 뉴스 조회",
		description = "사용자의 관심사 키워드에 기반한 맞춤 뉴스를 키워드별로 분류하여 제공합니다. " +
		             "관심사가 설정되지 않은 경우 기본 메시지를 반환합니다."
	)
	@GetMapping
	public ResponseEntity<?> getMainPageNews(Authentication authentication) {
		try {
			String username = null;
			if (authentication != null && authentication.isAuthenticated()) {
				username = authentication.getName();
			}

			MainPageResponseDto response = mainPageService.getPersonalizedMainPageNews(username);
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			// 오류 발생 시 기본 응답
			return ResponseEntity.ok(MainPageResponseDto.builder()
				.name("Guest")
				.message("뉴스를 불러오는 중 문제가 발생했습니다.")
				.hasPersonalizedNews(false)
				.build());
		}
	}

	@Operation(
		summary = "특정 키워드 뉴스 조회",
		description = "사용자의 특정 관심사 키워드에 해당하는 뉴스만 조회합니다."
	)
	@GetMapping("/keyword/{keyword}")
	public ResponseEntity<?> getNewsByKeyword(
			Authentication authentication,
			@Parameter(description = "조회할 키워드", example = "ui") 
			@PathVariable String keyword,
			@Parameter(description = "조회할 뉴스 개수", example = "10") 
			@RequestParam(defaultValue = "10") int limit) {
		try {
			String username = null;
			if (authentication != null && authentication.isAuthenticated()) {
				username = authentication.getName();
			}

			if (username == null) {
				return ResponseEntity.ok(MainPageResponseDto.builder()
					.name("Guest")
					.message("로그인이 필요한 서비스입니다.")
					.hasPersonalizedNews(false)
					.build());
			}

			MainPageResponseDto response = mainPageService.getNewsBySpecificKeyword(username, keyword, limit);
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			return ResponseEntity.ok(MainPageResponseDto.builder()
				.name("Guest")
				.message("키워드별 뉴스 조회 중 문제가 발생했습니다.")
				.hasPersonalizedNews(false)
				.build());
		}
	}

	@Operation(
		summary = "사용자 관심사 기반 뉴스 검색",
		description = "관리자용 API: 사용자의 모든 관심사를 기반으로 관련 뉴스를 검색합니다."
	)
	@GetMapping("/search")
	public ResponseEntity<?> searchNewsByInterests(
			Authentication authentication,
			@Parameter(description = "조회할 뉴스 개수", example = "20") 
			@RequestParam(defaultValue = "20") int limit) {
		try {
			String username = null;
			if (authentication != null && authentication.isAuthenticated()) {
				username = authentication.getName();
			}

			if (username == null) {
				return ResponseEntity.badRequest().body("로그인이 필요합니다.");
			}

			var news = mainPageService.searchNewsByInterests(username, limit);
			return ResponseEntity.ok(news);
			
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("뉴스 검색 중 오류가 발생했습니다.");
		}
	}
}
