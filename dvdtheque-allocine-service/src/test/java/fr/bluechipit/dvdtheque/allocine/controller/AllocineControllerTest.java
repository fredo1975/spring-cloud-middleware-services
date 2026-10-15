package fr.bluechipit.dvdtheque.allocine.controller;


import fr.bluechipit.dvdtheque.allocine.config.test.HazelcastConfiguration;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.dto.FicheFilmRec;
import fr.bluechipit.dvdtheque.allocine.service.AllocineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {HazelcastConfiguration.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AllocineControllerTest {
	protected Logger 								logger = LoggerFactory.getLogger(AllocineControllerTest.class);
	@Autowired
	private MockMvc 								mockmvc;
	@MockitoBean
	private JwtDecoder 								jwtDecoder;
	@MockitoBean
	private AllocineService allocineService;

	private FicheFilm ficheFilm(int id, String title) {
		var ficheFilm = new FicheFilm(title, id, "url-" + id, 1);
		ficheFilm.setId(id);
		return ficheFilm;
	}

	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmById() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilm(org.mockito.Mockito.anyInt()))
				.thenReturn(Optional.of(ficheFilm(1, "title")));
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byId")
				.param("id", String.valueOf(1))
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.id").value(1))
		.andExpect(jsonPath("$.allocineFilmId").value(1))
		.andExpect(jsonPath("$.title").value("title"))
		.andExpect(jsonPath("$.url").value("url-1"));
	}

	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmById_WhenNotFound_ShouldReturn404() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilm(org.mockito.Mockito.anyInt()))
				.thenReturn(Optional.empty());
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byId")
				.param("id", String.valueOf(999))
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmByTitle() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilmByTitle(anyString()))
				.thenReturn(List.of(ficheFilm(1, "title")));
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byTitle")
				.param("title", "title")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.length()").value(1))
		.andExpect(jsonPath("$[0].title").value("title"));
	}

	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmByTitle_WhenEmpty_ShouldFallBackToAllocine() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilmByTitle(anyString()))
				.thenReturn(List.of());
		Mockito.when(allocineService.extractFicheFilm(anyString()))
				.thenReturn(Optional.of(ficheFilm(7, "scarface")));

		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byTitle")
				.param("title", "scarface")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.length()").value(1))
		.andExpect(jsonPath("$[0].title").value("scarface"));

		verify(allocineService).extractFicheFilm("scarface");
	}

	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmByTitle_WhenOriginalTitleMatches_ShouldReturnIt() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilmByTitle("title")).thenReturn(List.of());
		Mockito.when(allocineService.retrieveFicheFilmByTitle("original")).thenReturn(List.of(ficheFilm(3, "original")));

		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byTitle")
				.param("title", "title")
				.param("titleO", "original")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$[0].title").value("original"));

		verify(allocineService, Mockito.never()).extractFicheFilm(anyString());
	}

	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmByTitle_WhenNothingFoundAnywhere_ShouldReturnEmptyList() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilmByTitle(anyString())).thenReturn(List.of());
		Mockito.when(allocineService.extractFicheFilm(anyString())).thenReturn(Optional.empty());

		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byTitle")
				.param("title", "unknown")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@WithMockUser(roles = "user")
	public void paginatedSearch() throws Exception {
		var film = new FicheFilm("title",1,"url",1);
		film.setId(1);
		var dto = FicheFilmRec.fromEntity(film);
		var l = new ArrayList<FicheFilmRec>();
		l.add(dto);
		Mockito.when(allocineService.paginatedSearch(anyString(),anyInt(),anyInt(),anyString()))
		.thenReturn(new PageImpl<FicheFilmRec>(l,PageRequest.of(0, 10),l.size()));
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/paginatedSarch")
				.param("query", "")
				.param("offset", String.valueOf(1))
				.param("limit", String.valueOf(20))
				.param("sort", "")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.content.length()").value(1))
		.andExpect(jsonPath("$.content[0].title").value("title"));
	}

	@Test
	@WithMockUser(roles = "batch")
	public void launchAllocineScrapingFicheFilm_ShouldReturn204() throws Exception {
		mockmvc.perform(MockMvcRequestBuilders.post("/dvdtheque-allocine-service/scraping-fichefilm")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isNoContent());

		verify(allocineService).scrapAllAllocineFicheFilmMultithreaded();
	}
}
