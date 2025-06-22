package challkahthon.backend.hihigh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response, 
                                         @org.springframework.web.bind.annotation.RequestParam(required = false) String refresh_token) {
        // Extract refresh token from request parameter or cookies
        String refreshToken = refresh_token;

        // If not provided as parameter, try to get from cookies
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = extractRefreshTokenFromCookies(request);
        }

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

        // Return the new access token in a structured JSON response
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
        return "success"; // templates/success.html
    }
}
