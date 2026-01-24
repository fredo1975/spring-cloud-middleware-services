package fr.bluechipit.dvdtheque.service;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.allocine.model.CritiquePresseDto;
import fr.bluechipit.dvdtheque.allocine.model.FicheFilmDto;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.dao.repository.FilmDao;
import fr.bluechipit.dvdtheque.dao.repository.GenreDao;
import fr.bluechipit.dvdtheque.dao.specifications.filter.SpecificationsBuilder;
import fr.bluechipit.dvdtheque.model.ExcelFilmHandler;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tmdb.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Classe utilitaire pour la gestion des dates (copiée pour les tests)
 */
class DateUtils {
    public final static String TMDB_DATE_PATTERN = "yyyy-MM-dd";

    public static Date clearDate(Date dateToClear) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateToClear);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTime();
    }

    public static Date parseDate(String dateStr, String pattern) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.FRANCE);
        return sdf.parse(dateStr);
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("FilmService Unit Tests")
public class FilmServiceTest {
    @Mock
    private FilmDao filmDao;

    @Mock
    private GenreDao genreDao;

    @Mock
    private PersonneService personneService;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private ExcelFilmHandler excelFilmHandler;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Environment environment;

    @Mock
    private FilmSaveService filmSaveService;

    @Mock
    private IMap<Long, Genre> mapGenres;

    @Mock
    private SpecificationsBuilder<Film> builder;

    private FilmService filmService;

    private Genre genreAction;
    private Genre genreDrama;
    private Film testFilm;
    private Results testResults;

    @BeforeEach
    void setUp() {
        // Configuration des mocks de base
        when(hazelcastInstance.<Long, Genre>getMap(FilmService.CACHE_GENRE)).thenReturn(mapGenres);
        when(genreDao.findAll()).thenReturn(new ArrayList<>());

        // Initialisation du service
        filmService = new FilmService(
                filmDao,
                null, // DvdDao non utilisé dans les tests
                genreDao,
                personneService,
                hazelcastInstance,
                excelFilmHandler,
                restTemplate,
                environment,
                filmSaveService,
                builder
        );

        // Données de test
        setupTestData();
    }

    private void setupTestData() {
        // Genres
        genreAction = new Genre();
        genreAction.setId(1L);
        genreAction.setTmdbId(28);
        genreAction.setName("Action");

        genreDrama = new Genre();
        genreDrama.setId(2L);
        genreDrama.setTmdbId(18);
        genreDrama.setName("Drama");

        // Film de test
        testFilm = new Film();
        testFilm.setId(1L);
        testFilm.setTitre("INCEPTION");
        testFilm.setTitreO("INCEPTION");
        testFilm.setTmdbId(27205L);
        testFilm.setAnnee(2010);
        testFilm.setOrigine(FilmOrigine.DVD);

        // Results TMDB
        testResults = new Results(
                27205L,
                "Inception",
                "Inception",
                "/poster.jpg",
                "2010-07-16",
                "A thief who steals corporate secrets...",
                148,
                Arrays.asList(28L, 18L),
                "http://inception.com"
        );
    }

    // ==================== Tests d'initialisation ====================

    @Test
    @DisplayName("init() doit charger tous les genres dans le cache")
    void init_ShouldLoadAllGenresIntoCache() {
        // Given
        List<Genre> genres = Arrays.asList(genreAction, genreDrama);
        when(genreDao.findAll()).thenReturn(genres);
        when(hazelcastInstance.<Long, Genre>getMap(FilmService.CACHE_GENRE)).thenReturn(mapGenres);

        // When
        FilmService newService = new FilmService(
                filmDao, null, genreDao, personneService, hazelcastInstance,
                excelFilmHandler, restTemplate, environment, filmSaveService,builder
        );

        // Then
        verify(mapGenres).putIfAbsent(28L, genreAction);
        verify(mapGenres).putIfAbsent(18L, genreDrama);
    }

    // ==================== Tests de récupération ====================

    @Test
    @DisplayName("getAllFilmDtos() doit retourner une liste triée de FilmDto")
    void getAllFilmDtos_ShouldReturnSortedFilmDtoList() {
        // Given
        Film film1 = new Film();
        film1.setTitre("ZZZZ");
        Dvd dvd1 = new Dvd();
        dvd1.setId(1L);
        film1.setDvd(dvd1);
        Personne personne1 = new Personne();
        personne1.setNom("Actor One");
        film1.getActeur().add(personne1);
        Personne personne2 = new Personne();
        personne2.setNom("Producer One");
        film1.getRealisateur().add(personne2);
        Film film2 = new Film();
        Dvd dvd2 = new Dvd();
        dvd2.setId(2L);
        film2.setDvd(dvd2);
        film2.setTitre("AAAA");
        film2.getActeur().add(personne1);
        film2.getRealisateur().add(personne2);
        when(filmDao.findAll()).thenReturn(Arrays.asList(film1, film2));

        // When
        List<FilmDto> result = filmService.getAllFilmDtos();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitre()).isEqualTo("AAAA");
        assertThat(result.get(1).getTitre()).isEqualTo("ZZZZ");
    }

    @Test
    @DisplayName("getAllFilmDtos() doit retourner une liste vide en cas d'erreur")
    void getAllFilmDtos_ShouldReturnEmptyListOnError() {
        // Given
        when(filmDao.findAll()).thenThrow(new RuntimeException("Database error"));

        // When
        List<FilmDto> result = filmService.getAllFilmDtos();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findFilmByTitreWithoutSpecialsCharacters() doit retourner le film correspondant")
    void findFilmByTitreWithoutSpecialsCharacters_ShouldReturnMatchingFilm() {
        // Given
        String titre = "Inception";
        when(filmDao.findFilmByTitreWithoutSpecialsCharacters(titre)).thenReturn(testFilm);

        // When
        Film result = filmService.findFilmByTitreWithoutSpecialsCharacters(titre);

        // Then
        assertThat(result).isEqualTo(testFilm);
        verify(filmDao).findFilmByTitreWithoutSpecialsCharacters(titre);
    }

    // ==================== Tests de transformation TMDB ====================

    @Test
    @DisplayName("transformTmdbFilmToDvdThequeFilm() doit créer un nouveau film avec les données TMDB")
    void transformTmdbFilmToDvdThequeFilm_ShouldCreateNewFilmWithTmdbData() throws Exception {
        // Given
        when(mapGenres.get(28L)).thenReturn(genreAction);
        when(mapGenres.get(18L)).thenReturn(genreDrama);

        String tmdbServiceUrl = "http://tmdb-service/";
        String releaseEndpoint = "release-date";
        String creditsEndpoint = "credits";
        String posterUrl = "http://image.tmdb.org/t/p/w500";

        when(environment.getRequiredProperty("tmdb-service.url")).thenReturn(tmdbServiceUrl);
        when(environment.getRequiredProperty("tmdb-service.release.date")).thenReturn(releaseEndpoint);
        when(environment.getRequiredProperty("tmdb-service.get-credits")).thenReturn(creditsEndpoint);
        when(environment.getRequiredProperty("tmdb.poster.path.url")).thenReturn(posterUrl);
        when(environment.getRequiredProperty("batch.save.nb.acteurs")).thenReturn("5");

        Credits credits = createMockCredits();
        when(restTemplate.getForObject(anyString(), eq(Credits.class))).thenReturn(credits);
        when(restTemplate.getForObject(contains("release-date"), eq(Date.class)))
                .thenReturn(DateUtils.parseDate("2010-07-16", DateUtils.TMDB_DATE_PATTERN));
        when(environment.getRequiredProperty("themoviedb.poster.path.url"))
                .thenReturn("http://image.tmdb.org/");
        Personne actor = new Personne();
        actor.setId(1L);
        actor.setNom("LEONARDO DICAPRIO");
        when(personneService.buildPersonne(anyString(), anyString())).thenReturn(actor);

        Personne director = new Personne();
        director.setId(2L);
        director.setNom("CHRISTOPHER NOLAN");
        when(personneService.buildPersonne(anyString(), isNull())).thenReturn(director);

        // When
        Film result = filmService.transformTmdbFilmToDvdThequeFilm(
                null, testResults, new HashSet<>(), false
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitre()).isEqualTo("INCEPTION");
        assertThat(result.getTmdbId()).isEqualTo(27205L);
        assertThat(result.getAnnee()).isEqualTo(2010);
        assertThat(result.getGenre()).hasSize(2);
        assertThat(result.getPosterPath()).contains("poster.jpg");
    }

    @Test
    @DisplayName("transformTmdbFilmToDvdThequeFilm() doit marquer le film comme déjà dans la DVDthèque")
    void transformTmdbFilmToDvdThequeFilm_ShouldMarkFilmAsAlreadyInDvdtheque() throws Exception {
        // Given
        Set<Long> alreadyInSet = new HashSet<>(Arrays.asList(27205L));
        setupMocksForTransform();

        // When
        Film result = filmService.transformTmdbFilmToDvdThequeFilm(
                null, testResults, alreadyInSet, false
        );

        // Then
        assertThat(result.isAlreadyInDvdtheque()).isTrue();
    }

    @Test
    @DisplayName("transformTmdbFilmToDvdThequeFilm() doit retourner null si les crédits échouent")
    void transformTmdbFilmToDvdThequeFilm_ShouldReturnNullIfCreditsError() {
        // Given
        setupMocksForTransform();
        when(restTemplate.getForObject(anyString(), eq(Credits.class)))
                .thenThrow(new RuntimeException("API Error"));

        // When
        Film result = filmService.transformTmdbFilmToDvdThequeFilm(
                null, testResults, new HashSet<>(), false
        );

        // Then
        assertThat(result).isNull();
    }

    // ==================== Tests de sauvegarde ====================

    @Test
    @DisplayName("saveFilm() doit sauvegarder un nouveau film depuis TMDB")
    void saveFilm_ShouldSaveNewFilmFromTmdb() throws ParseException {
        // Given
        Long tmdbId = 27205L;
        String origine = "DVD";

        when(filmDao.checkIfTmdbFilmExists(tmdbId)).thenReturn(0);

        setupEnvironmentForSave();

        ResultsByTmdbId resultsByTmdbId = createResultsByTmdbId();
        when(restTemplate.getForObject(anyString(), eq(ResultsByTmdbId.class)))
                .thenReturn(resultsByTmdbId);

        setupMocksForTransform();

        ResponseEntity<List<FicheFilmDto>> allocineResponse = ResponseEntity.ok(
                Arrays.asList(createFicheFilmDto())
        );
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(allocineResponse);

        Film savedFilm = new Film();
        savedFilm.setId(100L);
        when(filmSaveService.saveNewFilm(any(Film.class))).thenReturn(savedFilm);

        // When
        Optional<Film> result = filmService.saveFilm(tmdbId, origine);

        // Then
        assertThat(result).isPresent();
        verify(filmSaveService).saveNewFilm(any(Film.class));
    }

    @Test
    @DisplayName("saveFilm() doit retourner Optional.empty si le film existe déjà")
    void saveFilm_ShouldReturnEmptyIfFilmExists() throws ParseException {
        // Given
        Long tmdbId = 27205L;
        when(filmDao.checkIfTmdbFilmExists(tmdbId)).thenReturn(1);

        // When
        Optional<Film> result = filmService.saveFilm(tmdbId, "DVD");

        // Then
        assertThat(result).isEmpty();
        verify(filmSaveService, never()).saveNewFilm(any());
    }

    // ==================== Tests de recherche ====================

    @Test
    @DisplayName("search() doit utiliser les paramètres par défaut si null")
    void search_ShouldUseDefaultParametersIfNull() {
        // Given
        String query = "titre:like:Inception";
        PageRequest pageRequest = PageRequest.of(0, 50);
        Page<Film> page = new PageImpl<>(Arrays.asList(testFilm));

        when(builder.with(query)).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Specification.class));
        when(filmDao.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        // When
        List<Film> result = filmService.search(query, null, null, null);

        // Then
        assertThat(result).hasSize(1);
        verify(filmDao).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("paginatedSarch() doit retourner tous les films si query vide")
    void paginatedSarch_ShouldReturnAllFilmsIfEmptyQuery() {
        // Given
        Page<Film> page = new PageImpl<>(Arrays.asList(testFilm));
        when(filmDao.findAll(any(PageRequest.class))).thenReturn(page);

        // When
        Page<Film> result = filmService.paginatedSarch("", 1, 10, "");

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(filmDao).findAll(any(PageRequest.class));
    }

    // ==================== Tests de vérification ====================

    @Test
    @DisplayName("checkIfTmdbFilmExists() doit retourner TRUE si le film existe")
    void checkIfTmdbFilmExists_ShouldReturnTrueIfExists() {
        // Given
        Long tmdbId = 27205L;
        when(filmDao.checkIfTmdbFilmExists(tmdbId)).thenReturn(1);

        // When
        Boolean result = filmService.checkIfTmdbFilmExists(tmdbId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("checkIfTmdbFilmExists() doit retourner FALSE si le film n'existe pas")
    void checkIfTmdbFilmExists_ShouldReturnFalseIfNotExists() {
        // Given
        Long tmdbId = 99999L;
        when(filmDao.checkIfTmdbFilmExists(tmdbId)).thenReturn(0);

        // When
        Boolean result = filmService.checkIfTmdbFilmExists(tmdbId);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Tests de mise à jour ====================

    @Test
    @DisplayName("updateFilm() doit mettre à jour le film et récupérer les critiques presse")
    void updateFilm_ShouldUpdateFilmAndRetrieveCritiques() {
        // Given
        Film filmToUpdate = new Film();
        filmToUpdate.setId(1L);
        filmToUpdate.setTitre("UPDATED TITLE");

        when(filmSaveService.updateFilm(filmToUpdate)).thenReturn(filmToUpdate);
        when(filmSaveService.findFilm(1L)).thenReturn(testFilm);

        testFilm.setAllocineFicheFilmId(123);

        FicheFilmDto ficheFilm = createFicheFilmDto();
        ResponseEntity<FicheFilmDto> response = ResponseEntity.ok(ficheFilm);

        when(environment.getRequiredProperty("allocine-service.url"))
                .thenReturn("http://allocine/");
        when(environment.getRequiredProperty("allocine-service.byId"))
                .thenReturn("film/byId");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        // When
        Film result = filmService.updateFilm(filmToUpdate, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(filmSaveService).updateFilm(filmToUpdate);
    }

    // ==================== Tests des genres ====================

    @Test
    @DisplayName("saveGenre() doit sauvegarder le genre et l'ajouter au cache")
    void saveGenre_ShouldSaveGenreAndAddToCache() {
        // Given
        Genre newGenre = new Genre();
        newGenre.setTmdbId(99);
        newGenre.setName("Horror");

        Genre savedGenre = new Genre();
        savedGenre.setId(10L);
        savedGenre.setTmdbId(99);
        savedGenre.setName("Horror");

        when(genreDao.save(newGenre)).thenReturn(savedGenre);

        // When
        Genre result = filmService.saveGenre(newGenre);

        // Then
        assertThat(result).isEqualTo(savedGenre);
        verify(mapGenres).putIfAbsent(10L, savedGenre);
    }

    @Test
    @DisplayName("findAllGenres() doit retourner les genres du cache triés")
    void findAllGenres_ShouldReturnSortedGenresFromCache() {
        // Given
        Collection<Genre> genres = Arrays.asList(genreDrama, genreAction);
        when(mapGenres.values()).thenReturn(genres);

        // When
        List<Genre> result = filmService.findAllGenres();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Action");
        assertThat(result.get(1).getName()).isEqualTo("Drama");
    }

    // ==================== Tests de suppression ====================

    @Test
    @DisplayName("removeFilm() doit supprimer le film")
    void removeFilm_ShouldDeleteFilm() {
        // Given
        Long filmId = 1L;
        when(filmSaveService.findFilm(filmId)).thenReturn(testFilm);

        // When
        filmService.removeFilm(filmId);

        // Then
        verify(filmDao).delete(testFilm);
    }

    @Test
    @DisplayName("cleanAllFilms() doit supprimer tous les films et personnes")
    void cleanAllFilms_ShouldDeleteAllFilmsAndPersonnes() {
        // When
        filmService.cleanAllFilms();

        // Then
        verify(filmDao).deleteAll();
        verify(personneService).cleanAllPersonnes();
    }

    // ==================== Méthodes utilitaires de test ====================

    private Credits createMockCredits() {
        Credits credits = new Credits();

        Cast cast = new Cast();
        cast.setName("Leonardo DiCaprio");
        cast.setCast_id("1");
        cast.setProfile_path("/profile.jpg");
        credits.setCast(Arrays.asList(cast));

        Crew crew = new Crew();
        crew.setName("Christopher Nolan");
        crew.setJob("Director");
        credits.setCrew(Arrays.asList(crew));

        return credits;
    }

    private ResultsByTmdbId createResultsByTmdbId() {
        Genres genre1 = new Genres(28L, "Action");
        Genres genre2 = new Genres(18L, "Drama");

        return new ResultsByTmdbId(
                27205L,
                "Inception",
                "Inception",
                "/poster.jpg",
                "2010-07-16",
                "A thief who steals corporate secrets...",
                148,
                Arrays.asList(genre1, genre2),
                "http://inception.com"
        );
    }

    private FicheFilmDto createFicheFilmDto() {
        FicheFilmDto dto = new FicheFilmDto();
        dto.setId(123);

        CritiquePresseDto critique = new CritiquePresseDto();
        critique.setAuthor("Le Monde");
        critique.setBody("Excellent film!");
        critique.setRating(4.5);
        critique.setNewsSource("Le Monde");

        dto.setCritiquePresse(new HashSet<>(Arrays.asList(critique)));
        return dto;
    }

    private void setupMocksForTransform() {
        when(mapGenres.get(28L)).thenReturn(genreAction);
        when(mapGenres.get(18L)).thenReturn(genreDrama);

        when(environment.getRequiredProperty("tmdb-service.url"))
                .thenReturn("http://tmdb/");
        when(environment.getRequiredProperty("tmdb-service.release.date"))
                .thenReturn("release-date");
        when(environment.getRequiredProperty("tmdb-poster.path.url"))
                .thenReturn("http://image.tmdb.org/");
        when(environment.getRequiredProperty("batch.save.nb.acteurs"))
                .thenReturn("5");
        when(environment.getRequiredProperty("tmdb-service.get-credits"))
                .thenReturn("credits");
        when(environment.getRequiredProperty("themoviedb.poster.path.url"))
                .thenReturn("http://image.tmdb.org/");

        Credits credits = createMockCredits();
        when(restTemplate.getForObject(anyString(), eq(Credits.class)))
                .thenReturn(credits);

        try {
            when(restTemplate.getForObject(contains("release-date"), eq(Date.class)))
                    .thenReturn(DateUtils.parseDate("2010-07-16", DateUtils.TMDB_DATE_PATTERN));
        } catch (Exception e) {
            // Ignore
        }

        Personne actor = new Personne();
        actor.setId(1L);
        when(personneService.buildPersonne(anyString(), anyString())).thenReturn(actor);

        Personne director = new Personne();
        director.setId(2L);
        when(personneService.buildPersonne(anyString(), isNull())).thenReturn(director);
    }

    private void setupEnvironmentForSave() {
        when(environment.getRequiredProperty("tmdb-service.url"))
                .thenReturn("http://tmdb/");
        when(environment.getRequiredProperty("tmdb-service.get-results"))
                .thenReturn("results");
        when(environment.getRequiredProperty("allocine-service.url"))
                .thenReturn("http://allocine/");
        when(environment.getRequiredProperty("allocine-service.byTitle"))
                .thenReturn("search");
    }
}
