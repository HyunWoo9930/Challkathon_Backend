package challkahthon.backend.hihigh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import challkahthon.backend.hihigh.dto.TokenValidationResponseDto;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "인증", description = "인증 관련 API")
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
		return "success";
	}

	@Operation(
		summary = "사용자 정보 조회",
		description = "현재 로그인된 사용자의 정보를 조회합니다. 비밀번호는 제외하고 반환됩니다."
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "사용자 정보 조회 성공",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponseDto.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@GetMapping("/user-info")
	public ResponseEntity<?> getUserInfo() {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
		summary = "토큰 유효성 검증",
		description = "제공된 JWT 토큰의 유효성을 검증하고 토큰 정보를 반환합니다."
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "토큰 유효성 검증 결과",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TokenValidationResponseDto.class)
			)
		)
	})
	@GetMapping("/validate-token")
	public ResponseEntity<TokenValidationResponseDto> validateToken(HttpServletRequest request) {
		// Authorization 헤더에서 토큰 추출
		String authHeader = request.getHeader("Authorization");
		String token = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		}

		// 토큰이 없는 경우
		if (token == null || token.trim().isEmpty()) {
			return ResponseEntity.ok(TokenValidationResponseDto.invalid("토큰이 제공되지 않았습니다."));
		}

		try {
			// 토큰 유효성 검증
			if (!tokenProvider.validateToken(token)) {
				return ResponseEntity.ok(TokenValidationResponseDto.invalid("유효하지 않은 토큰입니다."));
			}

			// 토큰이 만료되었는지 확인
			if (tokenProvider.isTokenExpired(token)) {
				return ResponseEntity.ok(TokenValidationResponseDto.invalid("토큰이 만료되었습니다."));
			}

			// 토큰 정보 추출
			String username = tokenProvider.getUserIdFromJWT(token);
			Date issuedAt = tokenProvider.getTokenIssuedAt(token);
			Date expiresAt = tokenProvider.getTokenExpiration(token);
			String tokenType = tokenProvider.getTokenType(token);

			// Date를 LocalDateTime으로 변환
			LocalDateTime issuedAtLocalDateTime = issuedAt.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
			LocalDateTime expiresAtLocalDateTime = expiresAt.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();

			// 사용자 존재 여부 확인
			User user = userDetailsService.findByLoginId(username);
			if (user == null) {
				return ResponseEntity.ok(TokenValidationResponseDto.invalid("토큰의 사용자를 찾을 수 없습니다."));
			}

			return ResponseEntity.ok(TokenValidationResponseDto.valid(
				user.getName(),
				issuedAtLocalDateTime,
				expiresAtLocalDateTime,
				tokenType != null ? tokenType.toUpperCase() : "ACCESS"
			));

		} catch (Exception e) {
			return ResponseEntity.ok(TokenValidationResponseDto.invalid("토큰 처리 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@Operation(
		summary = "사용자 추가 정보 업데이트",
		description = "사용자의 관심사, 목표, 희망직종 정보를 업데이트합니다. 로그인된 사용자만 사용 가능합니다."
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "업데이트할 사용자 정보",
		required = true,
		content = @io.swagger.v3.oas.annotations.media.Content(
			mediaType = "application/json",
			schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserUpdateDto.class)
		)
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 업데이트됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@PostMapping("/update-info")
	public ResponseEntity<?> updateUserInfo(@RequestBody UserUpdateDto updateDto) {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "업데이트할 관심사 정보",
		required = true,
		content = @io.swagger.v3.oas.annotations.media.Content(
			mediaType = "application/json",
			schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InterestsUpdateDto.class)
		)
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 업데이트됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@PutMapping("/interests")
	public ResponseEntity<?> updateUserInterests(@RequestBody InterestsUpdateDto updateDto) {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "업데이트할 목표 정보",
		required = true,
		content = @io.swagger.v3.oas.annotations.media.Content(
			mediaType = "application/json",
			schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GoalsUpdateDto.class)
		)
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 업데이트됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@PutMapping("/goals")
	public ResponseEntity<?> updateUserGoals(@RequestBody GoalsUpdateDto updateDto) {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "업데이트할 희망직종 정보",
		required = true,
		content = @io.swagger.v3.oas.annotations.media.Content(
			mediaType = "application/json",
			schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = DesiredOccupationUpdateDto.class)
		)
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 업데이트됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@PutMapping("/desired-occupation")
	public ResponseEntity<?> updateUserDesiredOccupation(@RequestBody DesiredOccupationUpdateDto updateDto) {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 삭제됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@DeleteMapping("/interests")
	public ResponseEntity<?> deleteUserInterests() {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 삭제됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@DeleteMapping("/goals")
	public ResponseEntity<?> deleteUserGoals() {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 삭제됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@DeleteMapping("/desired-occupation")
	public ResponseEntity<?> deleteUserDesiredOccupation() {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
	@io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "성공적으로 계정이 삭제됨",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 또는 사용자를 찾을 수 없음",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자",
			content = @io.swagger.v3.oas.annotations.media.Content(
				mediaType = "application/json",
				schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
			)
		)
	})
	@DeleteMapping("/account")
	public ResponseEntity<?> deleteAccount() {
		// 현재 인증된 사용자 정보 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
