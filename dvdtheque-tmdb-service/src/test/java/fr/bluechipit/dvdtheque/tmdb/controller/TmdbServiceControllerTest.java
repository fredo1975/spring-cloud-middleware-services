package fr.bluechipit.dvdtheque.tmdb.controller;

import fr.bluechipit.dvdtheque.tmdb.service.TmdbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import tmdb.model.Results;
import tmdb.model.ResultsByTmdbId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TmdbServiceController.class)
public class TmdbServiceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private TmdbService tmdbService;
	@MockBean
	private RestTemplate restTemplate;

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
}
