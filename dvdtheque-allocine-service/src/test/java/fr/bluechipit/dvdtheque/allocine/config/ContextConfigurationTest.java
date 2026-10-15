package fr.bluechipit.dvdtheque.allocine.config;

import fr.bluechipit.dvdtheque.allocine.dto.SearchResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ContextConfigurationTest {

    @Test
    @DisplayName("restTemplate is configured with a Jackson 3 JsonMessageConverter")
    void restTemplateContainsJacksonJsonConverter() {
        RestTemplate restTemplate = new ContextConfiguration().restTemplate();

        assertTrue(restTemplate.getMessageConverters().stream()
                .anyMatch(JacksonJsonHttpMessageConverter.class::isInstance));
    }

    @Test
    @DisplayName("restTemplate deserializes AlloCine autocomplete JSON that omits primitive fields")
    void restTemplateDeserializesMissingPrimitives() {
        RestTemplate restTemplate = new ContextConfiguration().restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://www.allocine.fr/_/autocomplete/scarface"))
                .andRespond(withSuccess(
                        "{\"error\":false,\"results\":[{\"label\":\"scarface\",\"data\":{\"id\":\"900\"}}]}",
                        MediaType.APPLICATION_JSON));

        SearchResponseDTO response = restTemplate.getForObject(
                "https://www.allocine.fr/_/autocomplete/scarface", SearchResponseDTO.class);

        assertFalse(response.error());
        assertEquals(1, response.results().size());
        assertEquals("scarface", response.results().get(0).label());
        assertEquals("900", response.results().get(0).data().id());
        // status/viewcount/irankpopular omitted in payload must map to 0, not fail
        assertEquals(0, response.results().get(0).status());
        server.verify();
    }
}
