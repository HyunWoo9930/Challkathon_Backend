package challkahthon.backend.hihigh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import challkahthon.backend.hihigh.domain.entity.User;
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
}
