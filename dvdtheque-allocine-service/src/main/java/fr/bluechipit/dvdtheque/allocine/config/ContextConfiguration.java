package fr.bluechipit.dvdtheque.allocine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ContextConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Absent values in Allocine responses must not fail deserialization: Jackson 3
        // defaults FAIL_ON_NULL_FOR_PRIMITIVES to true, whereas Jackson 2 mapped null
        // into primitive fields as 0.
        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();
        List<HttpMessageConverter<?>> converters = new ArrayList<>(restTemplate.getMessageConverters());
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof JacksonJsonHttpMessageConverter) {
                converters.set(i, new JacksonJsonHttpMessageConverter(jsonMapper));
            }
        }
        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }
}
