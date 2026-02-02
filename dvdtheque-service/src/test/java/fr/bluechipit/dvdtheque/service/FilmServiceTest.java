package fr.bluechipit.dvdtheque.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.repository.FilmDao;
import fr.bluechipit.dvdtheque.dao.repository.GenreDao;
import fr.bluechipit.dvdtheque.dao.specifications.filter.SpecificationsBuilder;
import fr.bluechipit.dvdtheque.model.ExcelFilmHandler;
import fr.bluechipit.dvdtheque.allocine.model.FicheFilmDto;
import fr.bluechipit.dvdtheque.model.FilmDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tmdb.model.Credits;
import tmdb.model.Results;
import tmdb.model.ResultsByTmdbId;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock private FilmDao filmDao;
    @Mock private GenreDao genreDao;
    @Mock private PersonneService personneService;
    @Mock private HazelcastInstance hazelcastInstance;
    @Mock private IMap<Long, Genre> mapGenres;
    @Mock private RestTemplate restTemplate;
    @Mock private Environment environment;
    @Mock private FilmSaveService filmSaveService;
    @Mock private ExcelFilmHandler excelFilmHandler;
    @Mock private SpecificationsBuilder<Film> builder;

    private FilmService filmService; // Retrait de @InjectMocks

    @BeforeEach
    void setUp() {
        // 1. Configurer le comportement d'Hazelcast AVANT l'instanciation
        lenient().when(hazelcastInstance.<Long, Genre>getMap(anyString())).thenReturn(mapGenres);
        lenient().when(mapGenres.size()).thenReturn(0);
        lenient().when(genreDao.findAll()).thenReturn(Collections.emptyList());

        // 2. Création manuelle du service (le constructeur appellera init() sans crash)
        filmService = new FilmService(
                filmDao, genreDao, personneService, hazelcastInstance,
                excelFilmHandler, restTemplate, environment,
                filmSaveService, builder
        );
    }

    @Test
    @DisplayName("Devrait transformer un résultat TMDB en entité Film")
    void transformTmdbFilmToDvdThequeFilmTest() {
        // GIVEN
        Results results = new Results(100L, "Inception", "Inception", "/path.jpg", "2010-07-16", "Desc", 148, List.of(1L), "url");
        given(environment.getRequiredProperty(anyString())).willReturn("http://image.url");

        // Mock des crédits (directeurs/acteurs) via RestTemplate
        Credits mockCredits = new Credits();
        mockCredits.setCast(Collections.emptyList());
        mockCredits.setCrew(Collections.emptyList());
        given(restTemplate.getForObject(anyString(), eq(Credits.class))).willReturn(mockCredits);

        // WHEN
        Film film = filmService.transformTmdbFilmToDvdThequeFilm(null, results, new HashSet<>(), false);

        // THEN
        assertThat(film).isNotNull();
        assertThat(film.getTitre()).isEqualTo("INCEPTION");
        assertThat(film.getTmdbId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Devrait effectuer une recherche paginée de DTOs")
    void paginatedDtoSearchTest() {
        // GIVEN
        Film film = new Film();
        film.setTitre("Test Movie");
        Page<Film> page = new PageImpl<>(List.of(film));

        given(filmDao.findAll(any(PageRequest.class))).willReturn(page);

        // WHEN
        Page<FilmDto> result = filmService.paginatedDtoSearch("", 1, 10, "titre");

        // THEN
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).titre()).isEqualTo("Test Movie");
    }

    @Test
    @DisplayName("Devrait sauvegarder un nouveau film après enrichissement Allocine")
    void saveFilmTest() {
        // GIVEN
        Long tmdbId = 99L;
        given(filmDao.checkIfTmdbFilmExists(tmdbId)).willReturn(0);

        // On simule les propriétés d'environnement nécessaires
        given(environment.getRequiredProperty(anyString())).willReturn("prop");

        // 1. Correction du mock pour la récupération du film TMDB
        ResultsByTmdbId tmdbRes = new ResultsByTmdbId(tmdbId, "Title", "TitleO", "/p", "2022-01-01", "Overview", 120, List.of(), "home");
        // Utilisez anyString() pour éviter les erreurs de concaténation de l'URL
        given(restTemplate.getForObject(anyString(), eq(ResultsByTmdbId.class))).willReturn(tmdbRes);

        // 2. IMPORTANT : Mock pour les CREDITS (ce qui faisait planter votre test)
        Credits mockCredits = new Credits();
        mockCredits.setCast(Collections.emptyList());
        mockCredits.setCrew(Collections.emptyList());
        given(restTemplate.getForObject(anyString(), eq(Credits.class))).willReturn(mockCredits);

        // 3. Mock pour l'enrichissement Allocine
        given(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(List.of(new FicheFilmDto())));

        // 4. Mock du save
        given(filmSaveService.saveNewFilm(any())).willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Optional<FilmDto> saved = filmService.saveFilm(tmdbId, "DVD");

        // THEN
        assertThat(saved).isPresent(); // Ne sera plus vide maintenant
        assertThat(saved.get().titre()).isEqualTo("TITLE");
    }

    @Test
    @DisplayName("Devrait récupérer tous les genres depuis le cache Hazelcast")
    void findAllGenresTest() {
        // GIVEN
        Genre g = new Genre();
        g.setName("Action");
        given(mapGenres.values()).willReturn(List.of(g));

        // WHEN
        List<Genre> genres = filmService.findAllGenres();

        // THEN
        assertThat(genres).hasSize(1);
        assertThat(genres.get(0).getName()).isEqualTo("Action");
    }

    @Test
    @DisplayName("Devrait supprimer un film et ses dépendances")
    void removeFilmTest() {
        // GIVEN
        Film f = new Film();
        given(filmSaveService.findFilm(1L)).willReturn(f);

        // WHEN
        filmService.removeFilm(1L);

        // THEN
        verify(filmDao).delete(f);
    }

    @Test
    @DisplayName("Devrait générer un export Excel")
    void exportFilmListTest() throws Exception {
        // GIVEN
        Page<Film> page = new PageImpl<>(List.of(new Film()));
        given(filmDao.findAll(any(PageRequest.class))).willReturn(page);
        given(excelFilmHandler.createByteContentFromFilmList(any())).willReturn(new byte[]{1, 2, 3});

        // WHEN
        byte[] excel = filmService.exportFilmList("TOUS");

        // THEN
        assertThat(excel).isNotEmpty();
    }
}