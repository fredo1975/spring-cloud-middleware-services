package fr.bluechipit.dvdtheque.batch.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthClientCredentialsRestTemplateInterceptorTest {

    private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);

    private ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("dvdtheque")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://fake/token")
                .build();
    }

    private OAuth2AuthorizedClient authorizedClient(ClientRegistration registration) {
        return new OAuth2AuthorizedClient(registration, "batch",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-abc",
                        Instant.now().minus(Duration.ofMinutes(1)), Instant.now().plus(Duration.ofMinutes(4))));
    }

    private HttpRequest request() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        return request;
    }

    @Test
    @DisplayName("intercept adds the bearer token from the client credentials flow")
    void intercept_ShouldAddBearerToken() throws Exception {
        ClientRegistration registration = registration();
        when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient(registration));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(any(), any())).thenReturn(response);

        OAuthClientCredentialsRestTemplateInterceptor interceptor =
                new OAuthClientCredentialsRestTemplateInterceptor(manager, registration);
        HttpRequest request = request();

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertThat(result).isSameAs(response);
        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-abc");
        verify(execution).execute(any(), any());
    }

    @Test
    @DisplayName("intercept fails when the client credentials flow returns no client")
    void intercept_WhenClientIsNull_ShouldThrow() {
        ClientRegistration registration = registration();
        when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        OAuthClientCredentialsRestTemplateInterceptor interceptor =
                new OAuthClientCredentialsRestTemplateInterceptor(manager, registration);

        assertThatThrownBy(() -> interceptor.intercept(request(), new byte[0], execution))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client credentials flow on keycloak failed");
    }
}
