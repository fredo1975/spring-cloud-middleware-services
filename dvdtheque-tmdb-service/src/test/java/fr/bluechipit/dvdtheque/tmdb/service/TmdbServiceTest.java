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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
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

    // Helper method to build the Record easily
    private Results createResults(Long id, String title, String date) {
        return new Results(
                id, title, title, "/path.jpg", date, "Overview", 120, List.of(), "http://home.com"
        );
    }
}
