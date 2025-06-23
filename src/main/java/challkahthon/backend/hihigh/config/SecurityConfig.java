package challkahthon.backend.hihigh.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import challkahthon.backend.hihigh.jwt.JwtAuthenticationEntryPoint;
import challkahthon.backend.hihigh.jwt.JwtAuthenticationFilter;
import challkahthon.backend.hihigh.jwt.OAuth2AuthenticationSuccessHandler;
import challkahthon.backend.hihigh.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint unauthorizedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomUserDetailsService customUserDetailsService;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

	public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		CustomUserDetailsService customUserDetailsService,
		OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler) {
		this.unauthorizedHandler = unauthorizedHandler;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.customUserDetailsService = customUserDetailsService;
		this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(
				sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedHandler))
			.authorizeHttpRequests(authorizeRequests -> authorizeRequests
				.requestMatchers("/", "/**").permitAll()
				.requestMatchers("/api/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/", "/oauth/**", "/login/**").permitAll()
				.anyRequest().authenticated()
			)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.oauth2Login(oauth2 -> oauth2
				.loginPage("/login")
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customUserDetailsService)
				)
				.successHandler(oAuth2AuthenticationSuccessHandler)
			);

		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws
		Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("*")); // 모든 오리진 허용 (개발용)
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L); // 1시간 캐시
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
