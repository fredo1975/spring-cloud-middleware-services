package fr.bluechipit.config.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ConfigServerEndpointsTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void servesApplicationLevelPropertiesFromNativeBackend() throws Exception {
		mockMvc.perform(get("/config-service/default"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("config-service"))
				.andExpect(jsonPath("$.profiles[0]").value("default"))
				.andExpect(jsonPath("$.propertySources[0].source['testapp.message']")
						.value("hello-from-native-repo"));
	}

	@Test
	void servesApplicationSpecificProperties() throws Exception {
		mockMvc.perform(get("/test-service/default"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("test-service"))
				.andExpect(jsonPath("$.propertySources[0].source['test-service.specific']")
						.value("value-for-test-service"));
	}

	@Test
	void returnsEmptyPropertySourcesForUnknownApp() throws Exception {
		mockMvc.perform(get("/unknown-app/default"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("unknown-app"))
				.andExpect(jsonPath("$.propertySources").isArray());
	}

	@Test
	void healthEndpointIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void prometheusEndpointIsExposed() throws Exception {
		mockMvc.perform(get("/actuator/prometheus"))
				.andExpect(status().isOk());
	}

	@Test
	void buildInfoEndpointExposesBuildMetadata() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.build").exists());
	}

}
