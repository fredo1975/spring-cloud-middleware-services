package fr.bluechipit.discovery;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false"
})
class DiscoveryServerEndpointsTest {

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private HttpResponse<String> get(String path, String accept) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.header("Accept", accept)
				.GET()
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void healthEndpointIsUp() throws Exception {
		HttpResponse<String> response = get("/actuator/health", "application/json");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat((String) JsonPath.read(response.body(), "$.status")).isEqualTo("UP");
	}

	@Test
	void buildInfoEndpointExposesBuildMetadata() throws Exception {
		HttpResponse<String> response = get("/actuator/info", "application/json");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat((String) JsonPath.read(response.body(), "$.build.artifact")).isEqualTo("discovery-service");
	}

	@Test
	void prometheusEndpointIsExposed() throws Exception {
		HttpResponse<String> response = get("/actuator/prometheus", "text/plain");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("jvm_memory_used_bytes");
	}

	@Test
	void eurekaServesItsRegistryEndpoint() throws Exception {
		HttpResponse<String> response = get("/eureka/apps", "application/json");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("applications");
	}

}
