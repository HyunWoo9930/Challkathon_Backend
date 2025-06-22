package challkahthon.backend.hihigh.jwt;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import challkahthon.backend.hihigh.domain.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final String redirectUri = "/";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        CustomOauth2UserDetails userDetails = (CustomOauth2UserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Generate access token
        String accessToken = tokenProvider.generateAccessToken(user);

        // Generate refresh token
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // Create a cookie with the access token
        Cookie accessTokenCookie = new Cookie("access_token", accessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(900); // 15 minutes (matching the token expiration)

        // Create a cookie with the refresh token
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setPath("/api/auth/refresh"); // Only sent to refresh endpoint
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(604800); // 7 days (matching the token expiration)

        // Add the cookies to the response
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        // Redirect to the frontend without the token in the URL
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
