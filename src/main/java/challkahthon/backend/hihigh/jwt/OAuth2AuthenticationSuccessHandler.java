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
    private final String redirectUri = "http://localhost:3000";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        CustomOauth2UserDetails userDetails = (CustomOauth2UserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Generate access token
        String accessToken = tokenProvider.generateAccessToken(user);

        // Generate refresh token
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // Build the redirect URL with tokens as query parameters
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();

        // Redirect to the frontend with tokens as query parameters
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
