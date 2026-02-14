package fr.bluechipit.dvdtheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.file.util.MultipartFileUtil;
import fr.bluechipit.dvdtheque.model.FilmDto;
import fr.bluechipit.dvdtheque.service.FilmSaveService;
import fr.bluechipit.dvdtheque.service.FilmService;
import fr.bluechipit.dvdtheque.service.PersonneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FilmController.class)
@WithMockUser(roles = "user")
@ActiveProfiles("test")
class FilmControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
}