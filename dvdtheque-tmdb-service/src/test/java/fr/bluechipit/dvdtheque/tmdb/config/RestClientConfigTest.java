package fr.bluechipit.dvdtheque.tmdb.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tmdb.model.Results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientConfigTest {

    @Test
    @DisplayName("restTemplate is configured with a Jackson 3 JsonMessageConverter")
    void restTemplateContainsJacksonJsonConverter() {
        RestTemplate restTemplate = new RestClientConfig().restTemplate();

        assertTrue(restTemplate.getMessageConverters().stream()
                .anyMatch(JacksonJsonHttpMessageConverter.class::isInstance));
    }

    @Test
    @DisplayName("restTemplate deserializes JSON that omits primitive fields")
    void restTemplateDeserializesMissingPrimitives() {
        RestTemplate restTemplate = new RestClientConfig().restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://api.tmdb.org/3/movie/1"))
                .andRespond(withSuccess("{\"id\":1,\"title\":\"Inception\"}", MediaType.APPLICATION_JSON));

        Results result = restTemplate.getForObject("http://api.tmdb.org/3/movie/1", Results.class);

        assertEquals(1L, result.id());
        assertEquals("Inception", result.title());
        assertEquals(0, result.runtime());
        server.verify();
    }
}
