package fr.bluechipit.dvdtheque.controller;

import tools.jackson.databind.json.JsonMapper;
import enums.DvdFormat;
import fr.bluechipit.dvdtheque.allocine.model.CritiquePresseDto;
import fr.bluechipit.dvdtheque.allocine.model.DvdBuilder;
import fr.bluechipit.dvdtheque.allocine.model.FicheFilmDto;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.file.util.MultipartFileUtil;
import fr.bluechipit.dvdtheque.model.FilmDto;
import fr.bluechipit.dvdtheque.service.FilmSaveService;
import fr.bluechipit.dvdtheque.service.FilmService;
import fr.bluechipit.dvdtheque.service.PersonneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import tmdb.model.Results;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = FilmController.class, excludeAutoConfiguration = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
@WithMockUser(roles = "user")
@ActiveProfiles("test")
class FilmControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper objectMapper;

	// Remplacement de @MockBean par @MockitoBean (Spring Boot 3.4+)
	@MockitoBean private FilmService filmService;
	@MockitoBean private FilmSaveService filmSaveService;
	@MockitoBean private PersonneService personneService;
	@MockitoBean private MultipartFileUtil multipartFileUtil;
	@MockitoBean private RestTemplate restTemplate;

	@Test
	@DisplayName("Devrait retourner la liste des genres")
	void findAllGenresTest() throws Exception {
		given(filmService.findAllGenres()).willReturn(List.of(new Genre()));

		mockMvc.perform(get("/dvdtheque-service/films/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait rechercher des films avec pagination")
	void paginatedSearchTest() throws Exception {
		FilmDto dto = new FilmDto(1L, 2024, null, null, null, "Inception", "Inception",
				null, null, false, null, null, null, null, null,
				false, null, null, null,
				Set.of(), Set.of(), Set.of(), null);
		Page<FilmDto> page = new PageImpl<>(List.of(dto));

		given(filmService.paginatedDtoSearch(anyString(), any(), any(), any())).willReturn(page);

		mockMvc.perform(get("/dvdtheque-service/films/paginatedSarch")
						.param("query", "test"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].titre").value("Inception"));
	}

	@Test
	@DisplayName("Devrait retourner un film par son ID")
	void findFilmByIdTest() throws Exception {
		Film film = new Film();
		film.setId(1L);
		film.setTitre("Titanic");

		given(filmService.processRetrieveCritiquePresse(eq(1L), any(), any())).willReturn(film);

		mockMvc.perform(get("/dvdtheque-service/films/byId/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Titanic"));
	}

	// --- Tests des méthodes PUT / POST (Modification & Création) ---

	@Test
	@DisplayName("Devrait mettre à jour un film")
	void updateFilmTest() throws Exception {
		Film film = new Film();
		film.setTitre("Avatar");

		given(filmService.updateFilm(any(Film.class), anyLong())).willReturn(film);

		mockMvc.perform(put("/dvdtheque-service/films/update/1")
						.with(csrf()) // Requis si CSRF est activé
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Avatar"));
	}

	@Test
	@WithMockUser(roles = "batch")
	@DisplayName("Devrait sauvegarder un film (Batch)")
	void saveFilmTest() throws Exception {
		FilmDto dto = new FilmDto(1L, 2024, null, null, null, "Tenet", "Tenet",
				null, null, false, null, null, null, null, null,
				false, null, null, null,
				Set.of(), Set.of(), Set.of(), null);

		given(filmService.saveFilm(anyLong(), anyString())).willReturn(Optional.of(dto));

		mockMvc.perform(put("/dvdtheque-service/films/save/12345")
						.with(csrf())
						.content("DVD")
						.contentType(MediaType.TEXT_PLAIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Tenet"));
	}

	// --- Tests Spécifiques (Upload & Export) ---

	@Test
	@DisplayName("Devrait importer une liste de films via Multipart")
	void importFilmListTest() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());

		mockMvc.perform(multipart("/dvdtheque-service/films/import").file(file).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Devrait exporter la liste des films en Excel")
	void exportFilmListTest() throws Exception {
		given(filmService.exportFilmList(anyString())).willReturn(new byte[10]);

		mockMvc.perform(post("/dvdtheque-service/films/export")
						.with(csrf())
						.content("DVD"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")));
	}

	// --- Tests de suppression et cache ---

	@Test
	@DisplayName("Devrait supprimer un film")
	void removeFilmTest() throws Exception {
		mockMvc.perform(put("/dvdtheque-service/films/remove/1").with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Devrait nettoyer les caches")
	void cleanCachesTest() throws Exception {
		mockMvc.perform(put("/dvdtheque-service/films/cleanCaches").with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Devrait rechercher des films")
	void searchTest() throws Exception {
		Film film = new Film();
		film.setTitre("Matrix");
		given(filmService.search(anyString(), anyInt(), anyInt(), anyString())).willReturn(List.of(film));

		mockMvc.perform(get("/dvdtheque-service/films/search")
						.param("query", "matrix").param("offset", "0")
						.param("limit", "10").param("sort", "titre"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait retourner une personne par son nom")
	void findPersonneTest() throws Exception {
		Personne personne = new Personne();
		personne.setNom("NOLAN");
		given(personneService.findPersonneByName("NOLAN")).willReturn(personne);

		mockMvc.perform(get("/dvdtheque-service/films/byPersonne").param("nom", "NOLAN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nom").value("NOLAN"));
	}

	@Test
	@DisplayName("Devrait nettoyer tous les films")
	void cleanAllFilmsTest() throws Exception {
		mockMvc.perform(put("/dvdtheque-service/films/cleanAllfilms").with(csrf()))
				.andExpect(status().isOk());

		verify(filmService).cleanAllFilms();
	}

	@Test
	@DisplayName("Devrait retourner les critiques presse Allocine par id")
	void critiquePresseAllocineByIdTest() throws Exception {
		CritiquePresseDto dto = new CritiquePresseDto();
		dto.setId(1);
		dto.setNewsSource("Le Monde");
		given(filmService.findAllCritiquePresseByAllocineFilmById(42)).willReturn(Set.of(dto));

		mockMvc.perform(get("/dvdtheque-service/films/allocine/byId").param("id", "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait retourner 404 si aucune critique presse Allocine par id")
	void critiquePresseAllocineByIdEmptyTest() throws Exception {
		given(filmService.findAllCritiquePresseByAllocineFilmById(42)).willReturn(Set.of());

		mockMvc.perform(get("/dvdtheque-service/films/allocine/byId").param("id", "42"))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Devrait retourner les fiches Allocine par titre")
	void critiquePresseAllocineByTitleTest() throws Exception {
		given(filmService.findAllCritiquePresseByAllocineFilmByTitle("Dune")).willReturn(List.of(new FicheFilmDto()));

		mockMvc.perform(get("/dvdtheque-service/films/allocine/byTitle").param("title", "Dune"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait retourner 404 si aucun résultat Allocine par titre")
	void critiquePresseAllocineByTitleEmptyTest() throws Exception {
		given(filmService.findAllCritiquePresseByAllocineFilmByTitle("Dune")).willReturn(List.of());

		mockMvc.perform(get("/dvdtheque-service/films/allocine/byTitle").param("title", "Dune"))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Devrait rechercher un film TMDB par titre")
	void findTmdbFilmByTitreTest() throws Exception {
		given(filmService.findTmdbFilmByTitre(eq("Dune"), isNull())).willReturn(List.of(new Film()));

		mockMvc.perform(get("/dvdtheque-service/films/tmdb/byTitre/Dune"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait rechercher un film TMDB par titre et page")
	void findTmdbFilmByTitreByPageTest() throws Exception {
		given(filmService.findTmdbFilmByTitre(eq("Dune"), eq(2))).willReturn(List.of(new Film()));

		mockMvc.perform(get("/dvdtheque-service/films/tmdb/byTitre/Dune/2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait vérifier l'existence d'un film TMDB")
	void checkIfTmdbFilmExistsTest() throws Exception {
		given(filmService.checkIfTmdbFilmExists(123L)).willReturn(true);

		mockMvc.perform(get("/dvdtheque-service/films/byTmdbId/123"))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));
	}

	@Test
	@DisplayName("Devrait retourner 500 si la vérification TMDB échoue")
	void checkIfTmdbFilmExistsErrorTest() throws Exception {
		given(filmService.checkIfTmdbFilmExists(123L)).willThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/dvdtheque-service/films/byTmdbId/123"))
				.andExpect(status().isInternalServerError());
	}

	@Test
	@DisplayName("Devrait retourner toutes les personnes")
	void findAllPersonneTest() throws Exception {
		given(personneService.findAllPersonne()).willReturn(List.of(new Personne()));

		mockMvc.perform(get("/dvdtheque-service/personnes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("Devrait retourner 500 si la récupération des personnes échoue")
	void findAllPersonneErrorTest() throws Exception {
		given(personneService.findAllPersonne()).willThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/dvdtheque-service/personnes"))
				.andExpect(status().isInternalServerError());
	}

	@Test
	@DisplayName("Devrait remplacer un film TMDB")
	void replaceFilmTest() throws Exception {
		Film film = new Film();
		film.setTitre("Dune");
		given(filmService.replaceFilm(any(Film.class), eq(5L))).willReturn(film);

		mockMvc.perform(put("/dvdtheque-service/films/tmdb/5")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Dune"));
	}

	@Test
	@DisplayName("Devrait transformer un film TMDB en film Dvdtheque")
	void transformTmdbFilmTest() throws Exception {
		Film film = new Film();
		film.setTitre("Dune");
		given(filmService.transformTmdbFilmToDvdThequeFilm(isNull(), any(Results.class), anySet(), eq(true)))
				.willReturn(film);

		Results results = new Results(1L, "Dune", "Dune", "/p.jpg", "2021-01-01", "overview", 155, List.of(28L), null);

		mockMvc.perform(put("/dvdtheque-service/transformTmdbFilmToDvdThequeFilm/tmdb/5")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(results)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Dune"));
	}

	@Test
	@DisplayName("Devrait récupérer l'image d'un film")
	void retrieveFilmImageTest() throws Exception {
		Film film = new Film();
		film.setTitre("Dune");
		given(filmService.retrieveFilmImage(1L)).willReturn(film);

		mockMvc.perform(put("/dvdtheque-service/films/retrieveImage/1").with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Dune"));
	}

	@Test
	@DisplayName("Devrait récupérer toutes les images de films")
	void retrieveAllFilmImagesTest() throws Exception {
		mockMvc.perform(put("/dvdtheque-service/films/retrieveAllImages").with(csrf()))
				.andExpect(status().isNoContent());

		verify(filmService).retrieveAllFilmImages();
	}

	@Test
	@WithMockUser(roles = "batch")
	@DisplayName("Devrait sauvegarder un film traité (Batch)")
	void saveProcessedFilmTest() throws Exception {
		Film film = new Film();
		film.setTitre("Dune");
		given(filmService.saveProcessedFilm(any(Film.class))).willReturn(new FilmDto(1L, 2024, null, null, null,
				"Dune", "Dune", null, null, false, null, null, null, null, null, false, null, null, null,
				Set.of(), Set.of(), Set.of(), null));

		mockMvc.perform(post("/dvdtheque-service/films/saveProcessedFilm")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre").value("Dune"));
	}

	@Test
	@DisplayName("Devrait construire un Dvd")
	void buildDvdTest() throws Exception {
		DvdBuilder builder = new DvdBuilder();
		Film film = new Film();
		film.setAnnee(2010);
		builder.setFilmToSave(film);
		builder.setZonedvd(2);
		builder.setFilmFormat("BLUERAY");
		given(filmService.buildDvd(eq(2010), eq(2), isNull(), isNull(), eq(DvdFormat.BLUERAY)))
				.willReturn(new Dvd());

		mockMvc.perform(post("/dvdtheque-service/films/buildDvd")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(builder)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Devrait mettre à jour une personne")
	void updatePersonneTest() throws Exception {
		Personne existing = new Personne();
		existing.setId(1L);
		existing.setNom("OLD");
		given(personneService.findByPersonneId(1L)).willReturn(existing);

		Personne payload = new Personne();
		payload.setNom("nolan");

		mockMvc.perform(put("/dvdtheque-service/personnes/byId/1")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isNoContent());

		verify(personneService).updatePersonne(existing);
	}

	@Test
	@DisplayName("Devrait retourner 404 si la personne à mettre à jour n'existe pas")
	void updatePersonneNotFoundTest() throws Exception {
		given(personneService.findByPersonneId(99L)).willReturn(null);

		Personne payload = new Personne();
		payload.setNom("nolan");

		mockMvc.perform(put("/dvdtheque-service/personnes/byId/99")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Devrait retourner tous les films")
	void getAllFilmsTest() throws Exception {
		given(filmService.getAllFilms()).willReturn(List.of(new FilmDto(1L, 2024, null, null, null,
				"Dune", "Dune", null, null, false, null, null, null, null, null, false, null, null, null,
				Set.of(), Set.of(), Set.of(), null)));

		mockMvc.perform(get("/dvdtheque-service/films"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].titre").value("Dune"));
	}

	@Test
	@DisplayName("Devrait exporter une recherche de films")
	void exportFilmSearchTest() throws Exception {
		given(filmService.search(anyString(), anyInt(), anyInt(), anyString())).willReturn(List.of(new Film()));
		given(filmService.exportFilmSearch(anyString())).willReturn(new byte[10]);

		mockMvc.perform(post("/dvdtheque-service/films/search/export")
						.with(csrf())
						.content("query"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")));
	}

	@Test
	@DisplayName("Devrait retourner 500 si la recherche d'export est nulle")
	void exportFilmSearchNullTest() throws Exception {
		given(filmService.search(anyString(), anyInt(), anyInt(), anyString())).willReturn(null);

		mockMvc.perform(post("/dvdtheque-service/films/search/export")
						.with(csrf())
						.content("query"))
				.andExpect(status().isInternalServerError());
	}
}