package fr.bluechipit.dvdtheque.tmdb.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        // 1. Setup the Connection Manager with pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);             // Max total connections
        connectionManager.setDefaultMaxPerRoute(20);    // Max connections per host (TMDB)

        // 2. Configure timeouts to prevent hanging
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(5000))
                .setConnectTimeout(Timeout.ofMilliseconds(5000))
                .build();

        // 3. Create the HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 4. Wrap it in a RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(factory);

        // NaN/absent values in TMDB responses (e.g. "runtime" omitted in search results)
        // must not fail deserialization: Jackson 3 defaults FAIL_ON_NULL_FOR_PRIMITIVES to
        // true, whereas Jackson 2 mapped null into primitive fields as 0.
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
