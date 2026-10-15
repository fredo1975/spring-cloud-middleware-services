package fr.bluechipit.dvdtheque.batch.writer;

import enums.FilmOrigine;
import model.Film;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.jms.Topic;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DbFilmWriterTest {

    private JmsTemplate jmsTemplate;
    private RestTemplate restTemplate;
    private AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager;
    private DbFilmWriter writer;

    @BeforeEach
    void setUp() {
        jmsTemplate = mock(JmsTemplate.class);
        restTemplate = mock(RestTemplate.class);
        authorizedClientManager = mock(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);

        MockEnvironment environment = new MockEnvironment()
                .withProperty(DbFilmWriter.DVDTHEQUE_SERVICE_URL, "http://fake/dvdtheque-service")
                .withProperty(DbFilmWriter.DVDTHEQUE_SERVICE_SAVE_FILM, "/films/saveProcessedFilm");

        writer = new DbFilmWriter();
        ReflectionTestUtils.setField(writer, "jmsTemplate", jmsTemplate);
        ReflectionTestUtils.setField(writer, "topic", mock(Topic.class));
        ReflectionTestUtils.setField(writer, "environment", environment);
        ReflectionTestUtils.setField(writer, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(writer, "authorizedClientServiceAndManager", authorizedClientManager);

        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Film.class)))
                .thenAnswer(invocation -> ResponseEntity.ok((Film) ((HttpEntity<?>) invocation.getArgument(2)).getBody()));
    }

    private OAuth2AuthorizedClient authorizedClient() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("dvdtheque")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://fake/token")
                .build();
        return new OAuth2AuthorizedClient(registration, "batch",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-value",
                        Instant.now().minus(Duration.ofMinutes(1)), Instant.now().plus(Duration.ofMinutes(4))));
    }

    private Film film(int id, String titre) {
        Film film = new Film();
        film.setId((long) id);
        film.setTitre(titre);
        film.setAnnee(2020);
        film.setTmdbId((long) id);
        film.setOrigine(FilmOrigine.DVD);
        return film;
    }

    @Test
    void write_ShouldSendJmsMessagesAndCallRestServicePerFilm() throws Exception {
        Film first = film(1, "first");
        Film second = film(2, "second");

        writer.write(new Chunk<>(Arrays.asList(first, second)));

        verify(jmsTemplate, times(4)).convertAndSend(any(jakarta.jms.Destination.class), any(Object.class));
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Film.class));
        verify(authorizedClientManager, times(2)).authorize(any(OAuth2AuthorizeRequest.class));
    }

    @Test
    void write_ShouldSendBearerAuthorizationAndFilmBody() throws Exception {
        Film film = film(1, "first");

        writer.write(new Chunk<>(List.of(film)));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), eq(Film.class));
        HttpEntity<?> entity = captor.getValue();
        assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer token-value");
        assertThat(entity.getBody()).isSameAs(film);
    }

    @Test
    void write_WithNullFilmInChunk_ShouldThrowNullPointer() {
        Chunk<Film> chunk = new Chunk<>(Arrays.asList((Film) null));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> writer.write(chunk))
                .isInstanceOf(NullPointerException.class);
    }
}
