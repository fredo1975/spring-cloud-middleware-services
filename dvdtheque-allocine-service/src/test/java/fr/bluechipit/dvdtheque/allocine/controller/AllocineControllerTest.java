package fr.bluechipit.dvdtheque.allocine.controller;


import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.dto.FicheFilmDto;
import fr.bluechipit.dvdtheque.allocine.service.AllocineService;
import fr.bluechipit.dvdtheque.integration.allocine.config.HazelcastConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {HazelcastConfiguration.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AllocineControllerTest {
	protected Logger 								logger = LoggerFactory.getLogger(AllocineControllerTest.class);
	@Autowired
	private MockMvc 								mockmvc;
	@MockBean
	private JwtDecoder 								jwtDecoder;
	@MockBean
	private AllocineService allocineService;
	@Autowired
    private ModelMapper modelMapper;
	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmById() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilm(org.mockito.Mockito.anyInt())).thenReturn(Optional.of(new FicheFilm()));
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byId")
				.param("id", String.valueOf(0))
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	@Test
	@WithMockUser(roles = "batch")
	public void getAllocineFicheFilmByTitle() throws Exception {
		Mockito.when(allocineService.retrieveFicheFilmByTitle(org.mockito.Mockito.anyString())).thenReturn(List.of(new FicheFilm()));
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/byTitle")
				.param("title", "title")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	@WithMockUser(roles = "user")
	public void paginatedSarch() throws Exception {
		var film = new FicheFilm("title",1,"url",1);
		film.setId(1);
		var dto = modelMapper.map(film, FicheFilmDto.class);
		var l = new ArrayList<FicheFilmDto>();
		l.add(dto);
		Mockito.when(allocineService.paginatedSarch(anyString(),anyInt(),anyInt(),anyString()))
		.thenReturn(new PageImpl<FicheFilmDto>(l,PageRequest.of(0, 10),l.size()));
		mockmvc.perform(MockMvcRequestBuilders.get("/dvdtheque-allocine-service/paginatedSarch")
				.param("query", "")
				.param("offset", String.valueOf(1))
				.param("limit", String.valueOf(20))
				.param("sort", "")
				.with(jwt().jwt(builder -> builder.subject("test")))
				.with(csrf()))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
}