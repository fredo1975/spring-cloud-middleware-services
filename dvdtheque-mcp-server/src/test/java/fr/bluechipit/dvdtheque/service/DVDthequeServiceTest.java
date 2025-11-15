package fr.bluechipit.dvdtheque.service;

import fr.bluechipit.dvdtheque.model.Film;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DVDthequeServiceTest {
    @Mock
    private RestTemplate restTemplate;

    private DVDthequeService service;

    @BeforeEach
    void setUp() {
        service = new DVDthequeService(restTemplate);
        ReflectionTestUtils.setField(service, "baseUrl", "http://dvdtheque.example");
    }

    @Test
    void getFilmByTitre_returnsFilmWhenFound() {
        Film film = new Film(); // utilise le POJO du projet
        ResponseEntity<Film> response = new ResponseEntity<>(film, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://dvdtheque.example/film/MyTitle"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Film.class)))
                .thenReturn(response);

        Film result = service.getFilmByTitre("MyTitle");
        assertSame(film, result);
    }

    @Test
    void getFilmByTitre_returnsNullOnWebClientResponseException() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Film.class)))
                .thenThrow(WebClientResponseException.create(
                        404,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        null,
                        Charset.defaultCharset()));

        Film result = service.getFilmByTitre("Unknown");
        assertNull(result);
    }
}
