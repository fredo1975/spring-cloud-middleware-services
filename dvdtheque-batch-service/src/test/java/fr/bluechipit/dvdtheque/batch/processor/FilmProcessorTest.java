package fr.bluechipit.dvdtheque.batch.processor;

import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.batch.format.FilmCsvImportFormat;
import model.Dvd;
import model.DvdBuilder;
import model.Film;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FilmProcessorTest {

    private JmsTemplate jmsTemplate;
    private RestTemplate restTemplate;
    private AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager;
    private MockEnvironment environment;
    private FilmProcessor processor;

    @BeforeEach
    void setUp() {
        jmsTemplate = mock(JmsTemplate.class);
        restTemplate = mock(RestTemplate.class);
        authorizedClientManager = mock(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
        environment = new MockEnvironment()
                .withProperty(FilmProcessor.DVDTHEQUE_SERVICE_URL, "http://fake/dvdtheque-service")
                .withProperty(FilmProcessor.DVDTHEQUE_SERVICE_BUILD_DVD, "/films/buildDvd")
                .withProperty("rippedFlagTasklet.from.file", "true");

        processor = new FilmProcessor();
        ReflectionTestUtils.setField(processor, "environment", environment);
        ReflectionTestUtils.setField(processor, "jmsTemplate", jmsTemplate);
        ReflectionTestUtils.setField(processor, "topic", mock(Topic.class));
        ReflectionTestUtils.setField(processor, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(processor, "authorizedClientServiceAndManager", authorizedClientManager);

        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient());
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

    private FilmCsvImportFormat item(String origine) {
        FilmCsvImportFormat item = new FilmCsvImportFormat();
        item.setTmdbId(389L);
        item.setTitre("12 HOMMES EN COLERE");
        item.setOrigine(origine);
        item.setFilmFormat("DVD");
        item.setZonedvd(2);
        return item;
    }

    private void stubDvdResponse(Dvd dvd) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Dvd.class)))
                .thenReturn(ResponseEntity.ok(dvd));
    }

    @Test
    void process_DvdOrigin_WithRippedFromFile_ShouldPopulateDvdAndParseDates() throws Exception {
        environment.setProperty("rippedFlagTasklet.from.file", "true");
        Dvd dvd = new Dvd();
        stubDvdResponse(dvd);

        FilmCsvImportFormat item = item(FilmOrigine.DVD.name());
        item.setRipped("oui");
        item.setRipDate("05/02/2021");
        item.setVu("oui");
        item.setDateVue("30/01/2021");
        item.setDateInsertion("30/01/2021");

        Film film = processor.process(item);

        assertThat(film.getOrigine()).isEqualTo(FilmOrigine.DVD);
        assertThat(film.getDvd()).isSameAs(dvd);
        assertThat(film.getDvd().isRipped()).isTrue();
        assertThat(film.getDvd().getDateRip()).isEqualTo(LocalDate.of(2021, 2, 5));
        assertThat(film.isVu()).isTrue();
        assertThat(film.getDateVue()).isEqualTo(LocalDate.of(2021, 1, 30));
        assertThat(film.getDateInsertion()).isEqualTo(LocalDate.of(2021, 1, 30));
        assertThat(film.getTitreO()).isEqualTo("12 HOMMES EN COLERE");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), eq(Dvd.class));
        DvdBuilder builder = (DvdBuilder) captor.getValue().getBody();
        assertThat(builder.getFilmFormat()).isEqualTo("DVD");
        assertThat(builder.getZonedvd()).isEqualTo(2);
        assertThat(builder.getFilmToSave()).isSameAs(film);

        verify(jmsTemplate, times(2)).convertAndSend(any(jakarta.jms.Destination.class), any(Object.class));
    }

    @Test
    void process_DvdOrigin_WhenRippedFromFileDisabled_ShouldForceNotRipped() throws Exception {
        environment.setProperty("rippedFlagTasklet.from.file", "false");
        Dvd dvd = new Dvd();
        stubDvdResponse(dvd);

        FilmCsvImportFormat item = item(FilmOrigine.DVD.name());
        item.setRipped("oui");
        item.setRipDate("05/02/2021");

        Film film = processor.process(item);

        assertThat(film.getDvd().isRipped()).isFalse();
        assertThat(film.getDvd().getDateRip()).isNull();
    }

    @Test
    void process_DvdOrigin_WhenRippedEmpty_ShouldBeNotRipped() throws Exception {
        Dvd dvd = new Dvd();
        stubDvdResponse(dvd);

        FilmCsvImportFormat item = item(FilmOrigine.DVD.name());
        item.setRipped("");

        Film film = processor.process(item);

        assertThat(film.getDvd().isRipped()).isFalse();
    }

    @Test
    void process_DvdOrigin_WhenRippedNon_ShouldBeNotRippedAndNotParseRipDate() throws Exception {
        Dvd dvd = new Dvd();
        stubDvdResponse(dvd);

        FilmCsvImportFormat item = item(FilmOrigine.DVD.name());
        item.setRipped("non");
        item.setRipDate("05/02/2021");

        Film film = processor.process(item);

        assertThat(film.getDvd().isRipped()).isFalse();
        assertThat(film.getDvd().getDateRip()).isNull();
    }

    @Test
    void process_NonDvdOrigin_ShouldNotCallRestTemplate() throws Exception {
        FilmCsvImportFormat item = item(FilmOrigine.TV.name());

        Film film = processor.process(item);

        assertThat(film.getOrigine()).isEqualTo(FilmOrigine.TV);
        assertThat(film.getDvd()).isNull();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void process_WhenDateInsertionEmpty_ShouldDefaultTo2018_07_01() throws Exception {
        FilmCsvImportFormat item = item(FilmOrigine.TV.name());
        item.setDateInsertion("");

        Film film = processor.process(item);

        assertThat(film.getDateInsertion()).isEqualTo(LocalDate.of(2018, 7, 1));
    }

    @Test
    void process_WhenOptionalDatesEmpty_ShouldBeNull() throws Exception {
        FilmCsvImportFormat item = item(FilmOrigine.TV.name());
        item.setDateVue("");
        item.setDateSortieDvd("");

        Film film = processor.process(item);

        assertThat(film.getDateVue()).isNull();
        assertThat(film.getDateSortieDvd()).isNull();
    }

    @Test
    void process_WhenVuEmpty_ShouldBeFalse() throws Exception {
        FilmCsvImportFormat item = item(FilmOrigine.TV.name());
        item.setVu("");

        Film film = processor.process(item);

        assertThat(film.isVu()).isFalse();
    }

    @Test
    void process_WhenVuNon_ShouldBeFalse() throws Exception {
        FilmCsvImportFormat item = item(FilmOrigine.TV.name());
        item.setVu("non");

        Film film = processor.process(item);

        assertThat(film.isVu()).isFalse();
    }

    @Test
    void process_ShouldParseProvidedDateSortieDvd() throws Exception {
        FilmCsvImportFormat item = item(FilmOrigine.TV.name());
        item.setDateSortieDvd("24/06/2020");

        Film film = processor.process(item);

        assertThat(film.getDateSortieDvd()).isEqualTo(LocalDate.of(2020, 6, 24));
    }
}
