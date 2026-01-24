package fr.bluechipit.dvdtheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.bluechipit.dvdtheque.allocine.model.CritiquePresseDto;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.file.util.MultipartFileUtil;
import fr.bluechipit.dvdtheque.service.FilmSaveService;
import fr.bluechipit.dvdtheque.service.FilmService;
import fr.bluechipit.dvdtheque.service.PersonneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class FilmControllerTest {
	private MockMvc mockMvc;

	@Mock
	private Environment environment;

	@Mock
	private FilmService filmService;

	@Mock
	private FilmSaveService filmSaveService;

	@Mock
	private PersonneService personneService;

	@Mock
	private MultipartFileUtil multipartFileUtil;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private FilmController filmController;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(filmController).build();
		objectMapper = new ObjectMapper();
	}

	@Test
	void findPersonne_ShouldReturnPersonne_WhenPersonneExists() throws Exception {
		// Arrange
		String nom = "Spielberg";
		Personne personne = new Personne();
		personne.setId(1L);
		personne.setNom(nom);

		when(personneService.findPersonneByName(nom)).thenReturn(personne);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/byPersonne")
						.param("nom", nom))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.nom", is(nom)));

		verify(personneService, times(1)).findPersonneByName(nom);
	}

	@Test
	void findAllGenres_ShouldReturnGenreList() throws Exception {
		// Arrange
		List<Genre> genres = Arrays.asList(
				createGenre(1L, "Action"),
				createGenre(2L, "Drama")
		);

		when(filmService.findAllGenres()).thenReturn(genres);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name", is("Action")))
				.andExpect(jsonPath("$[1].name", is("Drama")));

		verify(filmService, times(1)).findAllGenres();
	}

	@Test
	void cleanAllFilms_ShouldCallService() throws Exception {
		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/films/cleanAllfilms"))
				.andExpect(status().isOk());

		verify(filmService, times(1)).cleanAllFilms();
	}

	@Test
	void findAllCritiquePresseByAllocineFilmById_ShouldReturnCritiques_WhenFound() throws Exception {
		// Arrange
		Integer allocineId = 123;
		Set<CritiquePresseDto> critiques = new HashSet<>();
		CritiquePresseDto critique = new CritiquePresseDto();
		critique.setNewsSource("Le Figaro");
		critiques.add(critique);

		when(filmService.findAllCritiquePresseByAllocineFilmById(allocineId)).thenReturn(critiques);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/allocine/byId")
						.param("id", allocineId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		verify(filmService, times(1)).findAllCritiquePresseByAllocineFilmById(allocineId);
	}

	@Test
	void findAllCritiquePresseByAllocineFilmById_ShouldReturnNotFound_WhenEmpty() throws Exception {
		// Arrange
		Integer allocineId = 999;
		when(filmService.findAllCritiquePresseByAllocineFilmById(allocineId))
				.thenReturn(new HashSet<>());

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/allocine/byId")
						.param("id", allocineId.toString()))
				.andExpect(status().isNotFound());

		verify(filmService, times(1)).findAllCritiquePresseByAllocineFilmById(allocineId);
	}

	@Test
	void findFilmById_ShouldReturnFilm() throws Exception {
		// Arrange
		Long filmId = 1L;
		Film film = createFilm(filmId, "The Matrix");

		when(filmService.processRetrieveCritiquePresse(eq(filmId), any(), isNull()))
				.thenReturn(film);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/byId/" + filmId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(filmId.intValue())))
				.andExpect(jsonPath("$.titre", is("The Matrix")));

		verify(filmService, times(1)).processRetrieveCritiquePresse(eq(filmId), any(), isNull());
	}

	@Test
	void checkIfTmdbFilmExists_ShouldReturnTrue_WhenFilmExists() throws Exception {
		// Arrange
		Long tmdbId = 603L;
		when(filmService.checkIfTmdbFilmExists(tmdbId)).thenReturn(true);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/byTmdbId/" + tmdbId))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		verify(filmService, times(1)).checkIfTmdbFilmExists(tmdbId);
	}

	@Test
	void checkIfTmdbFilmExists_ShouldReturnInternalServerError_WhenExceptionThrown() throws Exception {
		// Arrange
		Long tmdbId = 603L;
		when(filmService.checkIfTmdbFilmExists(tmdbId))
				.thenThrow(new RuntimeException("Database error"));

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/byTmdbId/" + tmdbId))
				.andExpect(status().isInternalServerError());

		verify(filmService, times(1)).checkIfTmdbFilmExists(tmdbId);
	}

	@Test
	void findAllPersonne_ShouldReturnPersonneList() throws Exception {
		// Arrange
		List<Personne> personnes = Arrays.asList(
				createPersonne(1L, "Nolan"),
				createPersonne(2L, "Tarantino")
		);

		when(personneService.findAllPersonne()).thenReturn(personnes);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/personnes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].nom", is("Nolan")));

		verify(personneService, times(1)).findAllPersonne();
	}

	@Test
	void search_ShouldReturnFilmSet() throws Exception {
		// Arrange
		String query = "matrix";
		List<Film> films = Collections.singletonList(createFilm(1L, "The Matrix"));

		when(filmService.search(query, 0, 10, "titre")).thenReturn(films);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/search")
						.param("query", query)
						.param("offset", "0")
						.param("limit", "10")
						.param("sort", "titre"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		verify(filmService, times(1)).search(query, 0, 10, "titre");
	}

	@Test
	void paginatedSearch_ShouldReturnPageOfFilms() throws Exception {
		// Arrange
		String query = "matrix";
		List<Film> films = Collections.singletonList(createFilm(1L, "The Matrix"));
		Page<Film> page = new PageImpl<>(films, PageRequest.of(0, 10), 1);

		when(filmService.paginatedSarch(query, 0, 10, "titre")).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/dvdtheque-service/films/paginatedSarch")
						.param("query", query)
						.param("offset", "0")
						.param("limit", "10")
						.param("sort", "titre"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.totalElements", is(1)));

		verify(filmService, times(1)).paginatedSarch(query, 0, 10, "titre");
	}

	@Test
	void updateFilm_ShouldReturnUpdatedFilm() throws Exception {
		// Arrange
		Long filmId = 1L;
		Film film = createFilm(filmId, "The Matrix Updated");

		when(filmService.updateFilm(any(Film.class), eq(filmId))).thenReturn(film);

		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/films/update/" + filmId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre", is("The Matrix Updated")));

		verify(filmService, times(1)).updateFilm(any(Film.class), eq(filmId));
	}

	@Test
	void removeFilm_ShouldReturnNoContent() throws Exception {
		// Arrange
		Long filmId = 1L;
		doNothing().when(filmService).removeFilm(filmId);

		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/films/remove/" + filmId))
				.andExpect(status().isNoContent());

		verify(filmService, times(1)).removeFilm(filmId);
	}

	@Test
	void saveFilm_ShouldReturnSavedFilm_WhenFilmExists() throws Exception {
		// Arrange
		Long tmdbId = 603L;
		String origine = "TMDB";
		Film film = createFilm(1L, "The Matrix");

		when(filmService.saveFilm(tmdbId, origine)).thenReturn(Optional.of(film));

		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/films/save/" + tmdbId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(origine))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre", is("The Matrix")));

		verify(filmService, times(1)).saveFilm(tmdbId, origine);
	}

	@Test
	void saveFilm_ShouldReturnNotFound_WhenFilmDoesNotExist() throws Exception {
		// Arrange
		Long tmdbId = 999L;
		String origine = "TMDB";

		when(filmService.saveFilm(tmdbId, origine)).thenReturn(Optional.empty());

		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/films/save/" + tmdbId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(origine))
				.andExpect(status().isNotFound());

		verify(filmService, times(1)).saveFilm(tmdbId, origine);
	}

	@Test
	void updatePersonne_ShouldReturnNoContent_WhenPersonneExists() throws Exception {
		// Arrange
		Long personneId = 1L;
		Personne existingPersonne = createPersonne(personneId, "Nolan");
		Personne updatedPersonne = createPersonne(personneId, "NOLAN UPDATED");

		when(personneService.findByPersonneId(personneId)).thenReturn(existingPersonne);
		doNothing().when(personneService).updatePersonne(any(Personne.class));

		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/personnes/byId/" + personneId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedPersonne)))
				.andExpect(status().isNoContent());

		verify(personneService, times(1)).findByPersonneId(personneId);
		verify(personneService, times(1)).updatePersonne(any(Personne.class));
	}

	@Test
	void updatePersonne_ShouldReturnNotFound_WhenPersonneDoesNotExist() throws Exception {
		// Arrange
		Long personneId = 999L;
		Personne personne = createPersonne(personneId, "Unknown");

		when(personneService.findByPersonneId(personneId)).thenReturn(null);

		// Act & Assert
		mockMvc.perform(put("/dvdtheque-service/personnes/byId/" + personneId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(personne)))
				.andExpect(status().isNotFound());

		verify(personneService, times(1)).findByPersonneId(personneId);
		verify(personneService, never()).updatePersonne(any(Personne.class));
	}

	@Test
	void exportFilmList_ShouldReturnExcelFile() throws Exception {
		// Arrange
		String origine = "TMDB";
		byte[] excelContent = "excel-content".getBytes();

		when(filmService.exportFilmList(origine)).thenReturn(excelContent);

		// Act & Assert
		mockMvc.perform(post("/dvdtheque-service/films/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(origine))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Disposition"))
				.andExpect(header().string("Content-Type",
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(excelContent));

		verify(filmService, times(1)).exportFilmList(origine);
	}

	// Helper methods to create test objects
	private Film createFilm(Long id, String titre) {
		Film film = new Film();
		film.setId(id);
		film.setTitre(titre);
		return film;
	}

	private Personne createPersonne(Long id, String nom) {
		Personne personne = new Personne();
		personne.setId(id);
		personne.setNom(nom);
		return personne;
	}

	private Genre createGenre(Long id, String nom) {
		Genre genre = new Genre();
		genre.setId(id);
		genre.setName(nom);
		return genre;
	}
}
