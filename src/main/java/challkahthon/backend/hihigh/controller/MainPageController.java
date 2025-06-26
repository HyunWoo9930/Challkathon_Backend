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
}
