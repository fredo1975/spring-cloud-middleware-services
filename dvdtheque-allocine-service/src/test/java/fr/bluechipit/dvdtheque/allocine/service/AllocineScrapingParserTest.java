package fr.bluechipit.dvdtheque.allocine.service;

import com.google.common.util.concurrent.RateLimiter;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.domain.Page;
import fr.bluechipit.dvdtheque.allocine.dto.EntityDataDTO;
import fr.bluechipit.dvdtheque.allocine.dto.SearchResponseDTO;
import fr.bluechipit.dvdtheque.allocine.dto.SearchResultDTO;
import fr.bluechipit.dvdtheque.allocine.repository.FicheFilmRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import specifications.filter.SpecificationsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AllocineScrapingParserTest {

    private static final String FILMS_PAGE_1_URL = "https://www.allocine.fr/films/";
    private static final String FILMS_PAGE_2_URL = "https://www.allocine.fr/films/?page=2";
    private static final String SCARFACE_CRITIQUE_URL =
            "https://www.allocine.fr/film/fichefilm-27918/critiques/presse/";
    private static final String AUTOCOMPLETE_URL =
            "https://www.allocine.fr/_/autocomplete/scarface";

    @Mock
    private FicheFilmRepository ficheFilmRepository;
    @Mock
    private HazelcastInstance hazelcastInstance;
    @Mock
    private Environment environment;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private SpecificationsBuilder<FicheFilm> builder;
    @Mock
    private IMap<Integer, FicheFilm> mapFicheFilms;
    @Mock
    private IMap<String, List<FicheFilm>> mapFicheFilmsByTitle;

    private java.util.concurrent.ExecutorService executorService;
    private AllocineService allocineService;
    private Document filmsDocument;
    private Document critiqueDocument;
    private Document emptyDocument;

    @BeforeEach
    void setUp() throws IOException {
        doReturn(mapFicheFilms).when(hazelcastInstance).getMap("ficheFilms");
        doReturn(mapFicheFilmsByTitle).when(hazelcastInstance).getMap("ficheFilmsByTitle");

        executorService = mock(java.util.concurrent.ExecutorService.class);
        allocineService = spy(new AllocineService(
                ficheFilmRepository,
                hazelcastInstance,
                environment,
                builder,
                executorService,
                restTemplate));
        ReflectionTestUtils.setField(allocineService, "nbParsedPage", 2);
        ReflectionTestUtils.setField(allocineService, "batchSize", 10);
        ReflectionTestUtils.setField(allocineService, "rateLimitMs", 100L);
        ReflectionTestUtils.setField(allocineService, "rateLimiter", RateLimiter.create(1000.0));

        filmsDocument = Jsoup.parse(fixture("/fixtures/films-page.html"), FILMS_PAGE_1_URL);
        critiqueDocument = Jsoup.parse(fixture("/fixtures/critique-presse.html"), SCARFACE_CRITIQUE_URL);
        emptyDocument = Jsoup.parse("<html><body><p>nothing to parse</p></body></html>", FILMS_PAGE_1_URL);
    }

    // ==================== Document parsing ====================

    @Test
    @DisplayName("parses film links and skips malformed ones")
    void extractFicheFilmsFromDocument_ParsesFilmLinksAndSkipsInvalidOnes() throws Exception {
        @SuppressWarnings("unchecked")
        Set<FicheFilm> films = (Set<FicheFilm>) invokeAllocine(
                "extractFicheFilmsFromDocument", new Class<?>[]{Document.class, int.class}, filmsDocument, 3);

        assertThat(films).hasSize(2);
        FicheFilm scarface = filmByAllocineId(films, 27918);
        assertThat(scarface.getTitle()).isEqualTo("Scarface");
        assertThat(scarface.getUrl()).isEqualTo(SCARFACE_CRITIQUE_URL);
        assertThat(scarface.getPageNumber()).isEqualTo(3);
        assertThat(filmByAllocineId(films, 123).getTitle()).isEqualTo("Inception");
    }

    @Test
    @DisplayName("returns an empty set when the page contains no film link")
    void extractFicheFilmsFromDocument_ReturnsEmptyForDocumentWithoutFilms() throws Exception {
        @SuppressWarnings("unchecked")
        Set<FicheFilm> films = (Set<FicheFilm>) invokeAllocine(
                "extractFicheFilmsFromDocument", new Class<?>[]{Document.class, int.class}, emptyDocument, 1);

        assertThat(films).isEmpty();
    }

    // ==================== Page retrieval ====================

    @Test
    @DisplayName("page 1 is fetched from the base film list url")
    void retrieveAllFicheFilmFromPage_PageOne_UsesFilmListUrl() throws Exception {
        doReturn(filmsDocument).when(allocineService).connect(anyString());

        @SuppressWarnings("unchecked")
        Set<FicheFilm> films = (Set<FicheFilm>) invokeAllocine(
                "retrieveAllFicheFilmFromPage", new Class<?>[]{Page.class}, new Page(1));

        assertThat(films).hasSize(2);
        verify(allocineService).connect(FILMS_PAGE_1_URL);
    }

    @Test
    @DisplayName("page 2 is fetched with a page query parameter")
    void retrieveAllFicheFilmFromPage_PageTwo_UsesPageParameter() throws Exception {
        doReturn(filmsDocument).when(allocineService).connect(anyString());

        @SuppressWarnings("unchecked")
        Set<FicheFilm> films = (Set<FicheFilm>) invokeAllocine(
                "retrieveAllFicheFilmFromPage", new Class<?>[]{Page.class}, new Page(2));

        assertThat(films).hasSize(2);
        verify(allocineService).connect(FILMS_PAGE_2_URL);
    }

    @Test
    @DisplayName("returns an empty set when the network call fails")
    void retrieveAllFicheFilmFromPage_OnIoException_ReturnsEmptySet() throws Exception {
        doThrow(new IOException("network down")).when(allocineService).connect(anyString());

        @SuppressWarnings("unchecked")
        Set<FicheFilm> films = (Set<FicheFilm>) invokeAllocine(
                "retrieveAllFicheFilmFromPage", new Class<?>[]{Page.class}, new Page(1));

        assertThat(films).isEmpty();
    }

    // ==================== Critique press extraction ====================

    @Test
    @DisplayName("extracts news sources, bodies, authors and ratings from the critique page")
    void fetchCritiques_ExtractsNewsSourcesBodiesRatingsAndAuthors() throws Exception {
        doReturn(critiqueDocument).when(allocineService).connect(anyString());
        FicheFilm film = new FicheFilm("Scarface", 27918, SCARFACE_CRITIQUE_URL, 1);

        @SuppressWarnings("unchecked")
        Map<Integer, CritiquePresse> critiques = (Map<Integer, CritiquePresse>) invokeAllocine(
                "fetchCritiques", new Class<?>[]{FicheFilm.class}, film);

        assertThat(critiques).isNotEmpty();
        assertThat(film.getCritiquePresse()).hasSize(4);

        CritiquePresse figaro = critiqueOf(film, "Le Figaro");
        assertThat(figaro.getBody()).isEqualTo("Un classique du gangster américain.");
        assertThat(figaro.getAuthor()).isEqualTo("Jean Dupont");
        assertThat(figaro.getRating()).isEqualTo(4.0);

        CritiquePresse television = critiqueOf(film, "Télérama");
        assertThat(television.getBody()).isEqualTo("Al Pacino inoubliable dans ce rôle.");
        assertThat(television.getAuthor()).isEqualTo("Marie Martin");
        assertThat(television.getRating()).isEqualTo(5.0);

        CritiquePresse inrockuptibles = critiqueOf(film, "Les Inrockuptibles");
        assertThat(inrockuptibles.getBody()).isEqualTo("Une mise en scène magistrale.");
        assertThat(inrockuptibles.getAuthor()).isEqualTo("...");
        assertThat(inrockuptibles.getRating()).isEqualTo(0.0);

        CritiquePresse cahiers = critiqueOf(film, "Cahiers du Cinéma");
        assertThat(cahiers.getBody()).isEqualTo("Le pire film de l'année.");
        assertThat(cahiers.getAuthor()).isEqualTo("Paul Durand");
        assertThat(cahiers.getRating()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("rating is derived from the stareval-stars rating class")
    void extractRating_ReturnsRatingBasedOnSelectorClass() throws Exception {
        assertThat(extractRatingOf("<div><div class='rating-mdl n50 stareval-stars'></div></div>")).isEqualTo(5.0);
        assertThat(extractRatingOf("<div><div class='rating-mdl n30 stareval-stars'></div></div>")).isEqualTo(3.0);
        assertThat(extractRatingOf("<div><div class='rating-mdl n10 stareval-stars'></div></div>")).isEqualTo(1.0);
        assertThat(extractRatingOf("<div><span>no rating here</span></div>")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("recovers on the second attempt after a transient network failure")
    void retrieveCritiquePresseMap_RecoversAfterTransientIoException() throws Exception {
        FicheFilm film = new FicheFilm("Scarface", 27918, SCARFACE_CRITIQUE_URL, 1);
        doThrow(new IOException("transient failure"))
                .doReturn(critiqueDocument)
                .when(allocineService).connect(anyString());

        @SuppressWarnings("unchecked")
        Map<Integer, CritiquePresse> critiques = (Map<Integer, CritiquePresse>) invokeAllocine(
                "retrieveCritiquePresseMap", new Class<?>[]{FicheFilm.class}, film);

        assertThat(critiques).isNotEmpty();
        verify(allocineService, times(2)).connect(anyString());
    }

    // ==================== shouldProcessFicheFilm ====================

    @Test
    @DisplayName("a film already present in the cache is not processed again")
    void shouldProcessFicheFilm_FalseWhenCached() throws Exception {
        FicheFilm film = new FicheFilm("Scarface", 27918, SCARFACE_CRITIQUE_URL, 1);
        when(mapFicheFilms.get(27918)).thenReturn(film);

        boolean process = (Boolean) invokeAllocine(
                "shouldProcessFicheFilm", new Class<?>[]{FicheFilm.class}, film);

        assertThat(process).isFalse();
        verifyNoInteractions(ficheFilmRepository);
    }

    @Test
    @DisplayName("a film already present in the database is not processed again")
    void shouldProcessFicheFilm_FalseWhenInDatabase() throws Exception {
        FicheFilm film = new FicheFilm("Scarface", 27918, SCARFACE_CRITIQUE_URL, 1);
        when(mapFicheFilms.get(27918)).thenReturn(null);
        when(ficheFilmRepository.findByFicheFilmId(27918)).thenReturn(film);

        boolean process = (Boolean) invokeAllocine(
                "shouldProcessFicheFilm", new Class<?>[]{FicheFilm.class}, film);

        assertThat(process).isFalse();
    }

    @Test
    @DisplayName("an unknown film is processed")
    void shouldProcessFicheFilm_TrueWhenNew() throws Exception {
        FicheFilm film = new FicheFilm("Scarface", 27918, SCARFACE_CRITIQUE_URL, 1);
        when(mapFicheFilms.get(27918)).thenReturn(null);
        when(ficheFilmRepository.findByFicheFilmId(27918)).thenReturn(null);

        boolean process = (Boolean) invokeAllocine(
                "shouldProcessFicheFilm", new Class<?>[]{FicheFilm.class}, film);

        assertThat(process).isTrue();
    }

    // ==================== Page processing orchestration ====================

    @Test
    @DisplayName("processPage saves every film holding critique press entries")
    void processPage_SavesFilmsWithCritiques() throws Exception {
        stubConnectByPage();

        AllocineService.PageResult result = (AllocineService.PageResult) invokeAllocine(
                "processPage", new Class<?>[]{Page.class}, new Page(1));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getSavedCount()).isEqualTo(2);

        ArgumentCaptor<List<FicheFilm>> captor = captor();
        verify(ficheFilmRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("processPage succeeds without saving when the page holds no film")
    void processPage_ReturnsSuccessWithZeroWhenPageHasNoFilms() throws Exception {
        doReturn(emptyDocument).when(allocineService).connect(anyString());

        AllocineService.PageResult result = (AllocineService.PageResult) invokeAllocine(
                "processPage", new Class<?>[]{Page.class}, new Page(1));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSavedCount()).isZero();
        verify(ficheFilmRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("sequential scraping processes every configured page")
    void scrapAllAllocineFicheFilm_ProcessesAllConfiguredPages() throws IOException {
        stubConnectByPage();

        assertThatCode(() -> allocineService.scrapAllAllocineFicheFilm())
                .doesNotThrowAnyException();

        verify(allocineService).connect(FILMS_PAGE_1_URL);
        verify(allocineService).connect(FILMS_PAGE_2_URL);
        verify(ficheFilmRepository, times(2)).saveAll(anyList());
    }

    // ==================== extractFicheFilm ====================

    @Test
    @DisplayName("saves and returns the film when the autocomplete label matches")
    void extractFicheFilm_FoundOnAutocomplete_SavesAndReturnsFilm() throws IOException {
        when(restTemplate.getForEntity(AUTOCOMPLETE_URL, SearchResponseDTO.class))
                .thenReturn(ResponseEntity.ok(autocompleteResponse("Scarface", "900")));
        doReturn(critiqueDocument).when(allocineService).connect(anyString());

        Optional<FicheFilm> result = allocineService.extractFicheFilm("scarface");

        assertThat(result).isPresent();
        FicheFilm film = result.get();
        assertThat(film.getAllocineFilmId()).isEqualTo(900);
        assertThat(film.getUrl()).isEqualTo("https://www.allocine.fr/film/fichefilm-900/critiques/presse/");
        verify(allocineService).connect("https://www.allocine.fr/film/fichefilm-900/critiques/presse/");
        verify(ficheFilmRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("returns empty when no autocomplete label matches the title")
    void extractFicheFilm_NoMatchingLabel_ReturnsEmpty() throws IOException {
        when(restTemplate.getForEntity(anyString(), eq(SearchResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(autocompleteResponse("Scarface", "900")));

        Optional<FicheFilm> result = allocineService.extractFicheFilm("batman");

        assertThat(result).isEmpty();
        verify(allocineService, never()).connect(anyString());
        verifyNoInteractions(ficheFilmRepository);
    }

    @Test
    @DisplayName("returns empty when the autocomplete response has no body")
    void extractFicheFilm_NullResponseBody_ReturnsEmpty() throws IOException {
        when(restTemplate.getForEntity(AUTOCOMPLETE_URL, SearchResponseDTO.class))
                .thenReturn(ResponseEntity.ok(null));

        Optional<FicheFilm> result = allocineService.extractFicheFilm("scarface");

        assertThat(result).isEmpty();
        verify(allocineService, never()).connect(anyString());
    }

    // ==================== Helpers ====================

    private void stubConnectByPage() throws IOException {
        doAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return url.contains("critiques/presse") ? critiqueDocument : filmsDocument;
        }).when(allocineService).connect(anyString());
    }

    private Object invokeAllocine(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = AllocineService.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(allocineService, args);
    }

    private Double extractRatingOf(String html) throws Exception {
        Element element = Jsoup.parse(html).selectFirst("div");
        return (Double) invokeAllocine("extractRating", new Class<?>[]{Element.class}, element);
    }

    private CritiquePresse critiqueOf(FicheFilm film, String newsSource) {
        return film.getCritiquePresse().stream()
                .filter(c -> newsSource.equals(c.getNewsSource()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no critique found for source " + newsSource));
    }

    private FicheFilm filmByAllocineId(Set<FicheFilm> films, int allocineFilmId) {
        return films.stream()
                .filter(f -> f.getAllocineFilmId() == allocineFilmId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no film found with allocine id " + allocineFilmId));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<FicheFilm>> captor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }

    private SearchResponseDTO autocompleteResponse(String label, String dataId) {
        EntityDataDTO data = new EntityDataDTO(dataId, null, null, null, null, null, null, null, null);
        SearchResultDTO result = new SearchResultDTO(
                null, null, null, label, null, null, null, 0, 0, 0, false, null,
                data, null, null, null, null, null, 0, false, null, null);
        return new SearchResponseDTO(false, null, List.of(result));
    }

    private String fixture(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertThat(is).as("fixture %s not found on classpath", path).isNotNull();
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }
}
