package fr.bluechipit.dvdtheque.batch.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2ClientConfigurationTest {

    private final ClientHttpRequestInterceptor interceptor =
            new OAuth2ClientConfiguration().restTemplate().getInterceptors().get(0);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HttpRequest request() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        return request;
    }

    private ClientHttpRequestExecution execution() throws Exception {
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));
        return execution;
    }

    @Test
    @DisplayName("restTemplate exposes a single bearer-token interceptor")
    void restTemplate_ShouldExposeOneInterceptor() {
        RestTemplate restTemplate = new OAuth2ClientConfiguration().restTemplate();

        assertThat(restTemplate.getInterceptors()).hasSize(1);
    }

    @Test
    @DisplayName("no authentication passes the request through untouched")
    void intercept_WithoutAuthentication_ShouldPassThrough() throws Exception {
        HttpRequest request = request();
        ClientHttpRequestExecution execution = execution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        verify(execution).execute(any(), any());
    }

    @Test
    @DisplayName("a non OAuth2 credential passes the request through untouched")
    void intercept_WithNonOAuth2Credentials_ShouldPassThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "password", "ROLE_user"));
        HttpRequest request = request();
        ClientHttpRequestExecution execution = execution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        verify(execution).execute(any(), any());
    }

    @Test
    @DisplayName("an OAuth2 credential sets the bearer token header")
    void intercept_WithOAuth2Credentials_ShouldSetBearerHeader() throws Exception {
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-xyz",
                Instant.now(), Instant.now().plusSeconds(300));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", token, List.of()));
        HttpRequest request = request();
        ClientHttpRequestExecution execution = execution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-xyz");
        verify(execution).execute(any(), any());
    }
}
