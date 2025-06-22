package challkahthon.backend.hihigh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.jwt.JwtTokenProvider;
import challkahthon.backend.hihigh.service.CustomUserDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Extract refresh token from cookies
        String refreshToken = extractRefreshTokenFromCookies(request);
        
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is missing");
        }
        
        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }
        
        // Get user from refresh token
        String username = tokenProvider.getUserIdFromJWT(refreshToken);
        User user = userDetailsService.findByUserName(username);
        
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        
        // Generate new access token
        String newAccessToken = tokenProvider.generateAccessToken(user);
        
        // Set new access token as cookie
        Cookie accessTokenCookie = new Cookie("access_token", newAccessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(900); // 15 minutes
        response.addCookie(accessTokenCookie);
        
        return ResponseEntity.ok().body("Token refreshed successfully");
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
}