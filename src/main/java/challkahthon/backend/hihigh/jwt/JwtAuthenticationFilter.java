package challkahthon.backend.hihigh.jwt;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws
		IOException,
		ServletException {
		String jwt = getJwtFromRequest(request);

		if (jwt != null && tokenProvider.validateToken(jwt)) {
			String loginId = tokenProvider.getUserIdFromJWT(jwt);

			User user = customUserDetailsService.findByLoginId(loginId);
			if (user != null) {
				UserDetails userDetails = new CustomOauth2UserDetails(user, null);
				JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails, null,
					userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else {
				throw new UsernameNotFoundException("User not found with loginId: " + loginId);
			}
		}

		filterChain.doFilter(request, response);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		// First, try to get the token from the Authorization header
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		// If not found in header, try to get it from cookies
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				// Check for the new access_token cookie first
				if ("access_token".equals(cookie.getName())) {
					return cookie.getValue();
				}
				// For backward compatibility, also check for the old token cookie
				if ("token".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}
}
