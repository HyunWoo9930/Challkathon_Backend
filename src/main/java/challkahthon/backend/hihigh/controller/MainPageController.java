package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.dto.response.MainPageResponseDto;
import challkahthon.backend.hihigh.service.MainPageService;
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

	@Operation(
		summary = "메인 페이지 뉴스 조회",
		description = "메인 페이지에 표시할 카테고리별 뉴스 목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<?> getMainPageNews(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		MainPageResponseDto response = mainPageService.getMainPageNews();
		response.setName(username);
		return ResponseEntity.ok(response);
	}
}