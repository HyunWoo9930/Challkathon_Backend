package challkahthon.backend.hihigh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.DesiredOccupationUpdateDto;
import challkahthon.backend.hihigh.dto.GoalsUpdateDto;
import challkahthon.backend.hihigh.dto.InterestsUpdateDto;
import challkahthon.backend.hihigh.dto.UserResponseDto;
import challkahthon.backend.hihigh.dto.UserUpdateDto;
import challkahthon.backend.hihigh.jwt.JwtTokenProvider;
import challkahthon.backend.hihigh.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "사용자 인증", description = "사용자 인증 및 프로필 관리 API")
public class AuthController {

	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailsService userDetailsService;

	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response,
		@org.springframework.web.bind.annotation.RequestParam(required = false) String refresh_token) {
		String refreshToken = refresh_token;

		if (refreshToken == null || refreshToken.isEmpty()) {
			refreshToken = extractRefreshTokenFromCookies(request);
		}

		if (refreshToken == null) {
			return ResponseEntity.badRequest().body("Refresh token is missing");
		}

		if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
			return ResponseEntity.badRequest().body("Invalid refresh token");
		}

		String username = tokenProvider.getUserIdFromJWT(refreshToken);
		User user = userDetailsService.findByUserName(username);

		if (user == null) {
			return ResponseEntity.badRequest().body("User not found");
		}

		String newAccessToken = tokenProvider.generateAccessToken(user);

		return ResponseEntity.ok().body(new challkahthon.backend.hihigh.jwt.JwtAuthenticationResponse(newAccessToken));
	}

	private String extractRefreshTokenFromCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refresh_token".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	@GetMapping("/success")
	public String success() {
		return "OAuth 로그인 성공!";
	}

	@Operation(
		summary = "사용자 정보 조회",
		description = "현재 로그인된 사용자의 정보를 조회합니다. 비밀번호는 제외하고 반환됩니다."
	)
	@GetMapping("/user-info")
	public ResponseEntity<?> getUserInfo(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User user = userDetailsService.findByLoginId(username);

		if (user == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		// UserResponseDto로 변환하여 반환 (비밀번호 제외)
		UserResponseDto userResponse = UserResponseDto.fromEntity(user);
		return ResponseEntity.ok().body(userResponse);
	}

	@Operation(
		summary = "사용자 추가 정보 업데이트",
		description = "사용자의 관심사, 목표, 희망직종 정보를 업데이트합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@PostMapping("/update-info")
	public ResponseEntity<?> updateUserInfo(@RequestBody UserUpdateDto updateDto, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.updateUserInfo(username, updateDto);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "사용자 관심사 업데이트",
		description = "사용자의 관심사 정보만 업데이트합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@PutMapping("/interests")
	public ResponseEntity<?> updateUserInterests(@RequestBody InterestsUpdateDto updateDto,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.updateUserInterests(username, updateDto);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "사용자 목표 업데이트",
		description = "사용자의 목표 정보만 업데이트합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@PutMapping("/goals")
	public ResponseEntity<?> updateUserGoals(@RequestBody GoalsUpdateDto updateDto, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.updateUserGoals(username, updateDto);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "사용자 희망직종 업데이트",
		description = "사용자의 희망직종 정보만 업데이트합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@PutMapping("/desired-occupation")
	public ResponseEntity<?> updateUserDesiredOccupation(@RequestBody DesiredOccupationUpdateDto updateDto,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.updateUserDesiredOccupation(username, updateDto);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "사용자 관심사 삭제",
		description = "사용자의 관심사 정보를 삭제합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@DeleteMapping("/interests")
	public ResponseEntity<?> deleteUserInterests(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.deleteUserInterests(username);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "사용자 목표 삭제",
		description = "사용자의 목표 정보를 삭제합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@DeleteMapping("/goals")
	public ResponseEntity<?> deleteUserGoals(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.deleteUserGoals(username);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "사용자 희망직종 삭제",
		description = "사용자의 희망직종 정보를 삭제합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@DeleteMapping("/desired-occupation")
	public ResponseEntity<?> deleteUserDesiredOccupation(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User updatedUser = userDetailsService.deleteUserDesiredOccupation(username);

		if (updatedUser == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		return ResponseEntity.ok().body(updatedUser);
	}

	@Operation(
		summary = "계정 삭제",
		description = "사용자 계정을 완전히 삭제합니다. 로그인된 사용자만 사용 가능합니다. 삭제 후에는 복구할 수 없습니다."
	)
	@DeleteMapping("/account")
	public ResponseEntity<?> deleteAccount(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		boolean deleted = userDetailsService.deleteUser(username);

		if (!deleted) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없거나 삭제할 수 없습니다.");
		}

		return ResponseEntity.ok().body("계정이 성공적으로 삭제되었습니다.");
	}
}
