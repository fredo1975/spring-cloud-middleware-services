package fr.bluechipit.dvdtheque.tmdb.controller;

import fr.bluechipit.dvdtheque.tmdb.service.TmdbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tmdb.model.Credits;
import tmdb.model.Results;
import tmdb.model.ResultsByTmdbId;
import tmdb.model.SearchResults;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TmdbServiceController.class, excludeAutoConfiguration = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
public class TmdbServiceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TmdbService tmdbService;
	@MockitoBean
	private CacheManager cacheManager;

	// Helper method to handle the 9-argument Record constructor for ResultsByTmdbId
	private ResultsByTmdbId createResultsByTmdbId(Long id, String title) {
		return new ResultsByTmdbId(
				id, title, title, "/path.jpg", "2024-01-01", "Overview", 120, Collections.emptyList(), "http://home.com"
		);
	}
	// Helper method for the Results record
	private Results createResults(Long id, String title) {
		return new Results(
				id, title, title, "/path.jpg", "2024-01-01", "Overview", 120, Collections.emptyList(), "http://home.com"
		);
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET /retrieveTmdbFilm/byTmdbId - Success")
	void shouldReturnMovieWhenIdExists() throws Exception {
		ResultsByTmdbId mockMovie = createResultsByTmdbId(123L, "Inception");
		when(tmdbService.fetchTmdbMovieById(123L)).thenReturn(Optional.of(mockMovie));

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbFilm/byTmdbId")
						.param("tmdbId", "123")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(123))
				.andExpect(jsonPath("$.title").value("Inception"));
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET /retrieveTmdbFilm/byTmdbId - Not Found")
	void shouldReturnNoContentWhenMovieDoesNotExist() throws Exception {
		when(tmdbService.fetchTmdbMovieById(anyLong())).thenReturn(Optional.empty());

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbFilm/byTmdbId")
						.param("tmdbId", "999"))
				.andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(roles = "batch")
	@DisplayName("GET /retrieveTmdbFilmListByTitle/byTitle - Success")
	void shouldReturnListByTitle() throws Exception {
		List<Results> mockList = List.of(createResults(1L, "Inception"));
		when(tmdbService.searchAllMoviesByTitle("Inception")).thenReturn(mockList);

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbFilmListByTitle/byTitle")
						.param("title", "Inception"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].title").value("Inception"));
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET /checkIfPosterExists - Success")
	void shouldReturnBooleanForPoster() throws Exception {
		when(tmdbService.remoteResourceExists(anyString())).thenReturn(true);

		mockMvc.perform(get("/dvdtheque-tmdb-service/checkIfPosterExists/byPosterPath")
						.param("posterPath", "test.jpg"))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET /retrieveTmdbFrReleaseDate/byTmdbId - Success")
	void shouldReturnReleaseDate() throws Exception {
		when(tmdbService.fetchBestReleaseDate(123L)).thenReturn(LocalDate.of(2024, 5, 1));

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbFrReleaseDate/byTmdbId")
						.param("tmdbId", "123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").value("2024-05-01"));
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET /retrieveTmdbCredits/byTmdbId - Success")
	void shouldReturnCredits() throws Exception {
		Credits credits = new Credits();
		credits.setId(1L);
		when(tmdbService.fetchTmdbCredits(1L)).thenReturn(credits);

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbCredits/byTmdbId")
						.param("tmdbId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	@WithMockUser(roles = "batch")
	@DisplayName("GET /retrieveTmdbSearchResultsByTitle/byTitle - Success without page")
	void shouldReturnSearchResultsWithoutPage() throws Exception {
		when(tmdbService.fetchSearchResultsByTitle("Inception", null)).thenReturn(createSearchResults());

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbSearchResultsByTitle/byTitle")
						.param("title", "Inception"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results[0].title").value("Inception"));
	}

	@Test
	@WithMockUser(roles = "batch")
	@DisplayName("GET /retrieveTmdbSearchResultsByTitle/byTitle - Success with page")
	void shouldReturnSearchResultsWithPage() throws Exception {
		when(tmdbService.fetchSearchResultsByTitle("Inception", 2)).thenReturn(createSearchResults());

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbSearchResultsByTitle/byTitle")
						.param("title", "Inception")
						.param("page", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results[0].title").value("Inception"));
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET /checkIfProfileImageExists/byPosterPath - Success")
	void shouldReturnBooleanForProfileImage() throws Exception {
		when(tmdbService.remoteResourceExists(anyString())).thenReturn(false);

		mockMvc.perform(get("/dvdtheque-tmdb-service/checkIfProfileImageExists/byPosterPath")
						.param("profilePath", "test.jpg"))
				.andExpect(status().isOk())
				.andExpect(content().string("false"));
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET endpoint - returns 400 when a required parameter is missing")
	void shouldReturnBadRequestWhenParamMissing() throws Exception {
		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbFilmListByTitle/byTitle"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "user")
	@DisplayName("GET endpoint - maps service failure to a 500 ApiError")
	void shouldReturnServerErrorOnFailure() throws Exception {
		when(tmdbService.searchAllMoviesByTitle("Boom")).thenThrow(new RuntimeException("Boom"));

		mockMvc.perform(get("/dvdtheque-tmdb-service/retrieveTmdbFilmListByTitle/byTitle")
						.param("title", "Boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Boom"))
				.andExpect(jsonPath("$.status").value("500 INTERNAL_SERVER_ERROR"));
	}

	// Helper method for the SearchResults object
	private SearchResults createSearchResults() {
		SearchResults searchResults = new SearchResults();
		searchResults.setResults(List.of(createResults(1L, "Inception")));
		searchResults.setTotal_pages(1);
		return searchResults;
	}
}
