package fr.bluechipit.dvdtheque.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("ContextConfiguration - Unit Tests")
class ContextConfigurationTest {

    private final ContextConfiguration config = new ContextConfiguration();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("restTemplate doit enregistrer un intercepteur")
    void restTemplate_ShouldRegisterInterceptor() {
        RestTemplate restTemplate = config.restTemplate();

        assertThat(restTemplate.getInterceptors()).hasSize(1);
    }

    @Test
    @DisplayName("l'intercepteur ne doit rien ajouter sans authentification")
    void interceptor_ShouldDoNothingWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/x"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], 200);
        given(execution.execute(any(), any())).willReturn(response);

        config.restTemplate().getInterceptors().get(0).intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    @DisplayName("l'intercepteur ne doit rien ajouter si les credentials ne sont pas un OAuth2Token")
    void interceptor_ShouldIgnoreNonOAuth2Credentials() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of()));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/x"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], 200);
        given(execution.execute(any(), any())).willReturn(response);

        config.restTemplate().getInterceptors().get(0).intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    @DisplayName("l'intercepteur doit propager le bearer token OAuth2")
    void interceptor_ShouldPropagateBearerToken() throws Exception {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "abc",
                Instant.now(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", accessToken, List.of()));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/x"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], 200);
        given(execution.execute(any(), any())).willReturn(response);

        config.restTemplate().getInterceptors().get(0).intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer abc");
    }
}
