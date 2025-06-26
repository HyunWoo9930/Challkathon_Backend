package challkahthon.backend.hihigh.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	
	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws IOException, ServletException {
		
		try {
			String jwt = getJwtFromRequest(request);

			if (StringUtils.hasText(jwt)) {
				// 토큰이 만료되었는지 먼저 확인
				if (tokenProvider.isTokenExpired(jwt)) {
					logger.debug("JWT token is expired for request: {}", request.getRequestURI());
					// 만료된 토큰은 무시하고 다음 필터로
					filterChain.doFilter(request, response);
					return;
				}

				// 토큰 유효성 검증
				if (tokenProvider.validateToken(jwt)) {
					String loginId = tokenProvider.getUserIdFromJWT(jwt);

					if (StringUtils.hasText(loginId)) {
						User user = customUserDetailsService.findByLoginId(loginId);
						if (user != null) {
							UserDetails userDetails = new CustomOauth2UserDetails(user, null);
							JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails, null,
								userDetails.getAuthorities());
							authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
							SecurityContextHolder.getContext().setAuthentication(authentication);
							
							logger.debug("Successfully authenticated user: {} for request: {}", loginId, request.getRequestURI());
						} else {
							logger.warn("User not found with loginId: {} for request: {}", loginId, request.getRequestURI());
						}
					}
				} else {
					logger.debug("Invalid JWT token for request: {}", request.getRequestURI());
				}
			}
		} catch (Exception ex) {
			logger.error("Could not set user authentication in security context for request: {}", request.getRequestURI(), ex);
			// 인증 실패 시에도 계속 진행 (다른 인증 방법이 있을 수 있음)
		}

		filterChain.doFilter(request, response);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		// 1. Authorization 헤더에서 토큰 확인
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		// 2. 쿠키에서 토큰 확인
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				// access_token 쿠키 우선 확인
				if ("access_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
					return cookie.getValue();
				}
				// 하위 호환성을 위해 token 쿠키도 확인
				if ("token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}
}
