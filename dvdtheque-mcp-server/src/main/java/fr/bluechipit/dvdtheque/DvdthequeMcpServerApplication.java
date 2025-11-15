package fr.bluechipit.dvdtheque;

import fr.bluechipit.dvdtheque.config.JwtTokenInterceptor;
import fr.bluechipit.dvdtheque.service.DVDthequeService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class DvdthequeMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DvdthequeMcpServerApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.interceptors(jwtInterceptor())
				.build();
	}
	@Bean
	public ClientHttpRequestInterceptor jwtInterceptor() {
		return new JwtTokenInterceptor();
	}
	@Bean
	public ToolCallbackProvider tools(DVDthequeService dVDthequeService) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(dVDthequeService)
				.build();
	}
}