package fr.bluechipit.dvdtheque.tmdb.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tmdb.model.*;

@ExtendWith(MockitoExtension.class)
public class TmdbServiceTest {
    @Mock
    private Environment environment;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TmdbService tmdbService;

    @BeforeEach
    void setUp() {
        // This property is used for general movie details (fetchMovieDetails)
        lenient().when(environment.getRequiredProperty("themoviedb.movie.query"))
                .thenReturn("http://api.tmdb.org/3/movie");

        // THIS IS THE MISSING ONE for the search method (searchAllMoviesByTitle)
        lenient().when(environment.getRequiredProperty("themoviedb.search.movie.query"))
                .thenReturn("http://api.tmdb.org/3/search/movie");

        lenient().when(environment.getRequiredProperty("themoviedb.api.key"))
                .thenReturn("fake-api-key");
    }

    @Test
    @DisplayName("Should prioritize FR release date over US and others")
    void fetchBestReleaseDate_PriorityLogic() {
        // 1. Create the specific date value object
        ReleaseDatesResultsValues frDateValue = new ReleaseDatesResultsValues();
        frDateValue.setRelease_date("2024-05-01T00:00:00.000Z");

        // 2. Setup the French Country result
        ReleaseDatesResults frCountry = new ReleaseDatesResults();
        frCountry.setIso_3166_1("FR");
        frCountry.setRelease_dates(List.of(frDateValue));

        // 3. Setup a US Country result (to test priority)
        ReleaseDatesResultsValues usDateValue = new ReleaseDatesResultsValues();
        usDateValue.setRelease_date("2024-04-01T00:00:00.000Z");

        ReleaseDatesResults usCountry = new ReleaseDatesResults();
        usCountry.setIso_3166_1("US");
        usCountry.setRelease_dates(List.of(usDateValue));

        // 4. Wrap in the root ReleaseDates object
        ReleaseDates mockResponse = new ReleaseDates();
        mockResponse.setResults(List.of(usCountry, frCountry));

        // Mock the RestTemplate call
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class)))
                .thenReturn(mockResponse);

        // WHEN
        LocalDate result = tmdbService.fetchBestReleaseDate(123L);

        // THEN
        // Even though US was first in the list, the logic should find FR
        assertEquals(LocalDate.of(2024, 5, 1), result);
    }

    @Test
    @DisplayName("Should return empty Optional when TMDB returns 404")
    void fetchTmdbMovieById_NotFound() {
        // GIVEN
        Long tmdbId = 123L;

        // We must simulate a 404 specifically
        when(restTemplate.getForObject(anyString(), eq(ResultsByTmdbId.class)))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        null,
                        null));

        // WHEN
        Optional<ResultsByTmdbId> result = tmdbService.fetchTmdbMovieById(tmdbId);

        // THEN
        assertTrue(result.isEmpty(), "Should return Optional.empty on 404");

        // Verify that the error was logged (Optional)
        // No exception should be thrown to the test runner here
    }

    @Test
    @DisplayName("Should return false when remote resource does not exist (404)")
    void remoteResourceExists_ReturnsFalseOn404() {
        // GIVEN
        doThrow(new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.NOT_FOUND))
                .when(restTemplate).headForHeaders(anyString());

        // WHEN
        boolean exists = tmdbService.remoteResourceExists("http://image.com/post.jpg");

        // THEN
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should aggregate multiple pages of results and sort by date")
    void searchAllMoviesByTitle_ShouldAggregateAndSort() {
        // 1. Setup Mock Data for Page 1
        Results moviePage1 = createResults(1L, "Movie A", "2023-01-01");
        SearchResults resp1 = new SearchResults();
        resp1.setResults(List.of(moviePage1));
        resp1.setTotal_pages(2); // Indicate there is a second page

        // 2. Setup Mock Data for Page 2
        Results moviePage2 = createResults(2L, "Movie B", "2024-01-01");
        SearchResults resp2 = new SearchResults();
        resp2.setResults(List.of(moviePage2));
        resp2.setTotal_pages(2);

        // 3. Mock sequential calls to restTemplate
        // The first call returns Page 1, the second returns Page 2
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class)))
                .thenReturn(resp1)
                .thenReturn(resp2);

        // 4. Execute the search
        List<Results> finalResults = tmdbService.searchAllMoviesByTitle("Inception");

        // 5. Assertions
        assertEquals(2, finalResults.size(), "Should have aggregated results from both pages");

        // Verify Sorting: 2024 (Movie B) should come BEFORE 2023 (Movie A)
        // because of .sorted(Comparator.comparing(Results::release_date).reversed())
        assertEquals("Movie B", finalResults.get(0).title());
        assertEquals("Movie A", finalResults.get(1).title());

        // Verify the loop executed exactly twice
        verify(restTemplate, times(2)).getForObject(anyString(), eq(SearchResults.class));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - falls back to US when FR is missing")
    void fetchBestReleaseDate_FallbackToUS() {
        ReleaseDates mockResponse = releaseDates(
                countryRelease("US", "2024-04-01T00:00:00.000Z"),
                countryRelease("DE", "2024-03-01T00:00:00.000Z"));
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class))).thenReturn(mockResponse);

        assertEquals(LocalDate.of(2024, 4, 1), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - falls back to first available when FR and US are missing")
    void fetchBestReleaseDate_FallbackToFirstAvailable() {
        ReleaseDates mockResponse = releaseDates(
                countryRelease("DE", "2024-03-01T00:00:00.000Z"),
                countryRelease("ES", "2024-02-01T00:00:00.000Z"));
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class))).thenReturn(mockResponse);

        assertEquals(LocalDate.of(2024, 3, 1), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - returns today when response is null")
    void fetchBestReleaseDate_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class))).thenReturn(null);

        assertEquals(LocalDate.now(), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - returns today when results are empty")
    void fetchBestReleaseDate_EmptyResults() {
        ReleaseDates mockResponse = new ReleaseDates();
        mockResponse.setResults(List.of());
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class))).thenReturn(mockResponse);

        assertEquals(LocalDate.now(), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - returns today when release_dates are empty")
    void fetchBestReleaseDate_EmptyReleaseDates() {
        ReleaseDatesResults country = new ReleaseDatesResults();
        country.setIso_3166_1("FR");
        country.setRelease_dates(List.of());
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class))).thenReturn(releaseDates(country));

        assertEquals(LocalDate.now(), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - returns today on malformed date")
    void fetchBestReleaseDate_MalformedDate() {
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class)))
                .thenReturn(releaseDates(countryRelease("FR", "not-a-date")));

        assertEquals(LocalDate.now(), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - returns today when RestTemplate fails")
    void fetchBestReleaseDate_RestException() {
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class)))
                .thenThrow(new RestClientException("boom"));

        assertEquals(LocalDate.now(), tmdbService.fetchBestReleaseDate(1L));
    }

    @Test
    @DisplayName("fetchTmdbCredits - returns credits")
    void fetchTmdbCredits_Success() {
        Credits credits = new Credits();
        credits.setId(5L);
        when(restTemplate.getForObject(anyString(), eq(Credits.class))).thenReturn(credits);

        assertEquals(5L, tmdbService.fetchTmdbCredits(5L).getId());
    }

    @Test
    @DisplayName("fetchTmdbCredits - rethrows RestClientException")
    void fetchTmdbCredits_RestException() {
        when(restTemplate.getForObject(anyString(), eq(Credits.class))).thenThrow(new RestClientException("boom"));

        assertThrows(RestClientException.class, () -> tmdbService.fetchTmdbCredits(5L));
    }

    @Test
    @DisplayName("fetchSearchResultsByTitle - defaults to page 1 when page is null")
    void fetchSearchResultsByTitle_NullPageDefaultsToOne() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class))).thenReturn(new SearchResults());

        tmdbService.fetchSearchResultsByTitle("Inception", null);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(SearchResults.class));
        assertTrue(urlCaptor.getValue().contains("page=1"));
        assertTrue(urlCaptor.getValue().contains("api_key=fake-api-key"));
        assertTrue(urlCaptor.getValue().contains("query=Inception"));
    }

    @Test
    @DisplayName("fetchSearchResultsByTitle - uses the provided page")
    void fetchSearchResultsByTitle_UsesProvidedPage() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class))).thenReturn(new SearchResults());

        tmdbService.fetchSearchResultsByTitle("Inception", 3);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(SearchResults.class));
        assertTrue(urlCaptor.getValue().contains("page=3"));
    }

    @Test
    @DisplayName("fetchSearchResultsByTitle - rethrows RestClientException")
    void fetchSearchResultsByTitle_RestException() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class))).thenThrow(new RestClientException("boom"));

        assertThrows(RestClientException.class, () -> tmdbService.fetchSearchResultsByTitle("Inception", 1));
    }

    @Test
    @DisplayName("remoteResourceExists - returns false for blank url")
    void remoteResourceExists_BlankUrl() {
        assertFalse(tmdbService.remoteResourceExists(" "));
        assertFalse(tmdbService.remoteResourceExists(null));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("remoteResourceExists - returns true when HEAD succeeds")
    void remoteResourceExists_Success() {
        when(restTemplate.headForHeaders(anyString())).thenReturn(HttpHeaders.EMPTY);

        assertTrue(tmdbService.remoteResourceExists("http://image.com/post.jpg"));
    }

    @Test
    @DisplayName("remoteResourceExists - returns false on generic exception")
    void remoteResourceExists_GenericException() {
        when(restTemplate.headForHeaders(anyString())).thenThrow(new RuntimeException("boom"));

        assertFalse(tmdbService.remoteResourceExists("http://image.com/post.jpg"));
    }

    @Test
    @DisplayName("fetchTmdbMovieById - returns movie when found")
    void fetchTmdbMovieById_Success() {
        ResultsByTmdbId movie = new ResultsByTmdbId(1L, "Inception", "Inception", "/p.jpg", "2024-01-01", "o", 120,
                List.of(), "http://home.com");
        when(restTemplate.getForObject(anyString(), eq(ResultsByTmdbId.class))).thenReturn(movie);

        Optional<ResultsByTmdbId> result = tmdbService.fetchTmdbMovieById(1L);

        assertTrue(result.isPresent());
        assertEquals("Inception", result.get().title());
    }

    @Test
    @DisplayName("fetchTmdbMovieById - rethrows non-404 RestClientException")
    void fetchTmdbMovieById_RestException() {
        when(restTemplate.getForObject(anyString(), eq(ResultsByTmdbId.class)))
                .thenThrow(new RestClientException("boom"));

        assertThrows(RestClientException.class, () -> tmdbService.fetchTmdbMovieById(1L));
    }

    @Test
    @DisplayName("searchAllMoviesByTitle - filters out entries without a release date")
    void searchAllMoviesByTitle_FiltersBlankReleaseDates() {
        Results withoutDate = new Results(1L, "NoDate", "NoDate", "/p.jpg", null, "o", 0, List.of(), "h");
        Results withDate = createResults(2L, "Dated", "2024-01-01");
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class)))
                .thenReturn(searchResults(List.of(withoutDate, withDate), 1));

        List<Results> result = tmdbService.searchAllMoviesByTitle("x");

        assertEquals(1, result.size());
        assertEquals("Dated", result.get(0).title());
    }

    @Test
    @DisplayName("searchAllMoviesByTitle - returns empty when results are null")
    void searchAllMoviesByTitle_NullResults() {
        SearchResults response = new SearchResults();
        response.setResults(null);
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class))).thenReturn(response);

        assertTrue(tmdbService.searchAllMoviesByTitle("x").isEmpty());
    }

    @Test
    @DisplayName("searchAllMoviesByTitle - returns empty when response is null")
    void searchAllMoviesByTitle_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class))).thenReturn(null);

        assertTrue(tmdbService.searchAllMoviesByTitle("x").isEmpty());
    }

    @Test
    @DisplayName("searchAllMoviesByTitle - caps paging at 10 pages")
    void searchAllMoviesByTitle_CapsAtTenPages() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class)))
                .thenReturn(searchResults(List.of(createResults(1L, "Movie", "2024-01-01")), 100));

        tmdbService.searchAllMoviesByTitle("x");

        verify(restTemplate, times(10)).getForObject(anyString(), eq(SearchResults.class));
    }

    @Test
    @DisplayName("searchAllMoviesByTitle - wraps failures in a runtime exception")
    void searchAllMoviesByTitle_WrapsException() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class))).thenThrow(new RestClientException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> tmdbService.searchAllMoviesByTitle("x"));
        assertEquals("TMDB Search failed", ex.getMessage());
    }

    @Test
    @DisplayName("fetchTmdbCredits - returns null when TMDB returns no payload")
    void fetchTmdbCredits_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(Credits.class))).thenReturn(null);

        assertNull(tmdbService.fetchTmdbCredits(5L));
    }

    @Test
    @DisplayName("searchAllMoviesByTitle - returns empty when results list is empty")
    void searchAllMoviesByTitle_EmptyResults() {
        when(restTemplate.getForObject(anyString(), eq(SearchResults.class)))
                .thenReturn(searchResults(List.of(), 1));

        assertTrue(tmdbService.searchAllMoviesByTitle("x").isEmpty());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(SearchResults.class));
    }

    @Test
    @DisplayName("fetchBestReleaseDate - returns today when the best country has no release dates")
    void fetchBestReleaseDate_BestCountryWithoutDates() {
        ReleaseDatesResults frCountry = new ReleaseDatesResults();
        frCountry.setIso_3166_1("FR");
        frCountry.setRelease_dates(List.of());
        when(restTemplate.getForObject(anyString(), eq(ReleaseDates.class))).thenReturn(releaseDates(frCountry));

        assertEquals(LocalDate.now(), tmdbService.fetchBestReleaseDate(1L));
    }

    // Helper method to build the Record easily
    private Results createResults(Long id, String title, String date) {
        return new Results(
                id, title, title, "/path.jpg", date, "Overview", 120, List.of(), "http://home.com"
        );
    }

    private SearchResults searchResults(List<Results> results, int totalPages) {
        SearchResults response = new SearchResults();
        response.setResults(results);
        response.setTotal_pages(totalPages);
        return response;
    }

    private ReleaseDatesResults countryRelease(String iso, String releaseDate) {
        ReleaseDatesResultsValues value = new ReleaseDatesResultsValues();
        value.setRelease_date(releaseDate);
        ReleaseDatesResults country = new ReleaseDatesResults();
        country.setIso_3166_1(iso);
        country.setRelease_dates(List.of(value));
        return country;
    }

    private ReleaseDates releaseDates(ReleaseDatesResults... countries) {
        ReleaseDates releaseDates = new ReleaseDates();
        releaseDates.setResults(List.of(countries));
        return releaseDates;
    }
}
