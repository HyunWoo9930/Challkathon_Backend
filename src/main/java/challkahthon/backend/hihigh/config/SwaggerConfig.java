package challkahthon.backend.hihigh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {

	@Value("${server.port:8080}")
	private int serverPort;

	@Bean
	public OpenAPI customOpenAPI() {
		Info info = new Info()
			.title("HiHigh Career News API")
			.version("1.0")
			.description("진로 관련 뉴스 큐레이션과 AI 채팅 기능을 제공하는 API입니다.")
			.contact(new Contact()
				.name("HiHigh Team")
				.email("hw62459930@gmail.com"));

		// 자동으로 현재 서버 환경 감지
		List<Server> servers = Arrays.asList(
			// 로컬 개발 환경
			new Server()
				.url("http://localhost:" + serverPort)
				.description("로컬 개발 서버"),

			// 프로덕션 환경 (설정된 경우)
			new Server()
				.url("https://hihigh.lion.it.kr")
				.description("프로덕션 서버"),

			// 동적 서버 감지 (현재 요청 기반)
			new Server()
				.url("/")
				.description("현재 서버 (자동 감지)")
		);

		return new OpenAPI()
			.info(info)
			.servers(servers)
			.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
			.components(new io.swagger.v3.oas.models.Components()
				.addSecuritySchemes("Bearer Authentication", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")));
	}
}
