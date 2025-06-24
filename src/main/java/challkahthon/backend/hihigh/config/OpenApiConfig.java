package challkahthon.backend.hihigh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenApiCustomizer serverOpenApiCustomizer() {
		return openApi -> {
			List<Server> servers = new ArrayList<>();

			try {
				// 현재 요청에서 서버 정보 자동 감지
				ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
				if (attributes != null) {
					HttpServletRequest request = attributes.getRequest();

					String scheme = request.getScheme();
					String serverName = request.getServerName();
					int serverPort = request.getServerPort();
					String contextPath = request.getContextPath();

					// 표준 포트인 경우 포트 번호 생략
					String portPart = "";
					if ((scheme.equals("http") && serverPort != 80) ||
						(scheme.equals("https") && serverPort != 443)) {
						portPart = ":" + serverPort;
					}

					String currentServerUrl = scheme + "://" + serverName + portPart + contextPath;

					// 현재 서버를 첫 번째로 추가 (기본 선택)
					servers.add(new Server()
						.url(currentServerUrl)
						.description("현재 서버 (자동 감지)"));
				}
			} catch (Exception e) {
				// 요청 컨텍스트를 가져올 수 없는 경우 기본값 사용
			}

			// 추가 서버들
			servers.add(new Server()
				.url("http://localhost:8080")
				.description("로컬 개발 서버"));

			servers.add(new Server()
				.url("https://hihigh.lion.it.kr")
				.description("프로덕션 서버"));

			// 상대 경로 서버 (현재 도메인 사용)
			servers.add(new Server()
				.url("/")
				.description("현재 도메인"));

			openApi.setServers(servers);
		};
	}
}
