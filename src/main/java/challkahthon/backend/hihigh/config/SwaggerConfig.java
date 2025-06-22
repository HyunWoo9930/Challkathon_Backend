package challkahthon.backend.hihigh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		Info info = new Info()
			.title("User Management API")
			.version("1.0")
			.description("API for managing users and their profile images.")
			.contact(new Contact()
				.name("HyunWoo9930")
				.email("hw62459930@gmail.com"));

		// Create servers for both HTTP and HTTPS
		Server httpServer = new Server()
			.url("http://{host}")
			.description("HTTP Server");

		Server httpsServer = new Server()
			.url("https://{host}")
			.description("HTTPS Server");

		io.swagger.v3.oas.models.servers.ServerVariables httpVariables = new io.swagger.v3.oas.models.servers.ServerVariables();
		io.swagger.v3.oas.models.servers.ServerVariable hostVariable = new io.swagger.v3.oas.models.servers.ServerVariable();
		hostVariable.setDefault("localhost:8080");
		hostVariable.setDescription("Server host (and port)");
		httpVariables.addServerVariable("host", hostVariable);

		httpServer.setVariables(httpVariables);
		httpsServer.setVariables(httpVariables);

		return new OpenAPI()
			.info(info)
			.servers(java.util.List.of(httpServer, httpsServer))
			.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
			.components(new io.swagger.v3.oas.models.Components()
				.addSecuritySchemes("Bearer Authentication", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")));
	}
}
