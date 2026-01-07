package fr.bluechipit.dvdtheque.service;

import enums.DvdFormat;
import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.DvdthequeRestApplication;
import fr.bluechipit.dvdtheque.config.HazelcastConfigurationTest;
import fr.bluechipit.dvdtheque.config.TestWebSocketConfig;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.dao.model.utils.FilmBuilder;
import fr.bluechipit.dvdtheque.model.PersonneDto;
import fr.bluechipit.dvdtheque.service.impl.IFilmService;
import fr.bluechipit.dvdtheque.service.impl.IPersonneService;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = {HazelcastConfigurationTest.class,
		TestWebSocketConfig.class, DvdthequeRestApplication.class})
@Transactional
public class PersonneServiceIntegrationTests {
	protected Logger logger = LoggerFactory.getLogger(PersonneServiceIntegrationTests.class);
	@Autowired
	protected IPersonneService personneService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	protected IFilmService filmService;
	@MockitoBean
	private JwtDecoder 			jwtDecoder;
	@BeforeEach
	public void cleanAllCaches() {
		personneService.cleanAllCaches();
	}
	
	@Test
	public void getPersonneVersusLoadPersonne() throws Exception {
		Genre genre1 = filmService.saveGenre(new Genre(28,"Action"));
		Genre genre2 = filmService.saveGenre(new Genre(35,"Comedy"));
		Film film = new FilmBuilder.Builder(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setTitreO(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setAct1Nom(FilmBuilder.ACT1_TMBD_ID_844)
				.setAct2Nom(FilmBuilder.ACT2_TMBD_ID_844)
				.setAct3Nom(FilmBuilder.ACT3_TMBD_ID_844)
				.setRipped(true)
				.setAnnee(FilmBuilder.ANNEE)
				.setDateSortie(FilmBuilder.FILM_DATE_SORTIE).setDateInsertion(FilmBuilder.FILM_DATE_INSERTION)
				.setDvdFormat(DvdFormat.DVD)
				.setOrigine(FilmOrigine.DVD)
				.setGenre1(genre1)
				.setGenre2(genre2)
				.setZone(2)
				.setRealNom(FilmBuilder.REAL_NOM_TMBD_ID_844)
				.setRipDate(FilmBuilder.createRipDate(FilmBuilder.RIP_DATE_OFFSET)).setDateSortieDvd(FilmBuilder.DVD_DATE_SORTIE)
				.setAllocineFicheFilmId(FilmBuilder.ALLOCINE_FICHE_FILM_ID_844).build();
		Long filmId = filmService.saveNewFilm(film);
		FilmBuilder.assertFilmIsNotNull(film, false,FilmBuilder.RIP_DATE_OFFSET, FilmOrigine.DVD, null, null, false);
		Assertions.assertNotNull(filmId);
		Personne personneByLoad = personneService.loadPersonne(film.getRealisateur().iterator().next().getId());
		Assertions.assertNotNull(personneByLoad);
		logger.debug("personneByLoad=" + personneByLoad);
	}
	@Test
	public void findPersonne() throws Exception {
		Genre genre1 = filmService.saveGenre(new Genre(28,"Action"));
		Genre genre2 = filmService.saveGenre(new Genre(35,"Comedy"));
		Film film = new FilmBuilder.Builder(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setTitreO(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setAct1Nom(FilmBuilder.ACT1_TMBD_ID_844)
				.setAct2Nom(FilmBuilder.ACT2_TMBD_ID_844)
				.setAct3Nom(FilmBuilder.ACT3_TMBD_ID_844)
				.setRipped(true)
				.setAnnee(FilmBuilder.ANNEE)
				.setDateSortie(FilmBuilder.FILM_DATE_SORTIE).setDateInsertion(FilmBuilder.FILM_DATE_INSERTION)
				.setDvdFormat(DvdFormat.DVD)
				.setOrigine(FilmOrigine.DVD)
				.setGenre1(genre1)
				.setGenre2(genre2)
				.setZone(2)
				.setRealNom(FilmBuilder.REAL_NOM_TMBD_ID_844)
				.setRipDate(FilmBuilder.createRipDate(FilmBuilder.RIP_DATE_OFFSET)).setDateSortieDvd(FilmBuilder.DVD_DATE_SORTIE)
				.setAllocineFicheFilmId(FilmBuilder.ALLOCINE_FICHE_FILM_ID_844).build();
		Long filmId = filmService.saveNewFilm(film);
		Assertions.assertNotNull(filmId);
		FilmBuilder.assertFilmIsNotNull(film, false,FilmBuilder.RIP_DATE_OFFSET, FilmOrigine.DVD, null, null, false);
		Personne personne = personneService.findByPersonneId(film.getRealisateur().iterator().next().getId());
		Assertions.assertNotNull(personne);
	}
	
	@Test
	public void findRealisateurByFilm() throws Exception {
		Genre genre1 = filmService.saveGenre(new Genre(28,"Action"));
		Genre genre2 = filmService.saveGenre(new Genre(35,"Comedy"));
		Film film = new FilmBuilder.Builder(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setTitreO(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setAct1Nom(FilmBuilder.ACT1_TMBD_ID_844)
				.setAct2Nom(FilmBuilder.ACT2_TMBD_ID_844)
				.setAct3Nom(FilmBuilder.ACT3_TMBD_ID_844)
				.setRipped(true)
				.setAnnee(FilmBuilder.ANNEE)
				.setDateSortie(FilmBuilder.FILM_DATE_SORTIE).setDateInsertion(FilmBuilder.FILM_DATE_INSERTION)
				.setDvdFormat(DvdFormat.DVD)
				.setOrigine(FilmOrigine.DVD)
				.setGenre1(genre1)
				.setGenre2(genre2)
				.setZone(2)
				.setRealNom(FilmBuilder.REAL_NOM_TMBD_ID_844)
				.setRipDate(FilmBuilder.createRipDate(FilmBuilder.RIP_DATE_OFFSET)).setDateSortieDvd(FilmBuilder.DVD_DATE_SORTIE)
				.setAllocineFicheFilmId(FilmBuilder.ALLOCINE_FICHE_FILM_ID_844).build();
		Long filmId = filmService.saveNewFilm(film);
		Assertions.assertNotNull(filmId);
		FilmBuilder.assertFilmIsNotNull(film, false,FilmBuilder.RIP_DATE_OFFSET, FilmOrigine.DVD, null, null, false);
		film = filmService.findFilm(film.getId());
		Assertions.assertNotNull(film);
		Assertions.assertNotNull(film.getTitre());
		Personne real = personneService.findByPersonneId(film.getRealisateur().iterator().next().getId());
		Assertions.assertNotNull(real);
	}
	@Test
	public void findAllPersonnes() throws Exception {
		Genre genre1 = filmService.saveGenre(new Genre(28,"Action"));
		Genre genre2 = filmService.saveGenre(new Genre(35,"Comedy"));
		Film film = new FilmBuilder.Builder(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setTitreO(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setAct1Nom(FilmBuilder.ACT1_TMBD_ID_844)
				.setAct2Nom(FilmBuilder.ACT2_TMBD_ID_844)
				.setAct3Nom(FilmBuilder.ACT3_TMBD_ID_844)
				.setRipped(true)
				.setAnnee(FilmBuilder.ANNEE)
				.setDateSortie(FilmBuilder.FILM_DATE_SORTIE).setDateInsertion(FilmBuilder.FILM_DATE_INSERTION)
				.setDvdFormat(DvdFormat.DVD)
				.setOrigine(FilmOrigine.DVD)
				.setGenre1(genre1)
				.setGenre2(genre2)
				.setZone(2)
				.setRealNom(FilmBuilder.REAL_NOM_TMBD_ID_844)
				.setRipDate(FilmBuilder.createRipDate(FilmBuilder.RIP_DATE_OFFSET)).setDateSortieDvd(FilmBuilder.DVD_DATE_SORTIE)
				.setAllocineFicheFilmId(FilmBuilder.ALLOCINE_FICHE_FILM_ID_844).build();
		Long filmId = filmService.saveNewFilm(film);
		Assertions.assertNotNull(filmId);
		FilmBuilder.assertFilmIsNotNull(film, false,FilmBuilder.RIP_DATE_OFFSET, FilmOrigine.DVD, null, null, false);
		List<Personne> personneList = personneService.findAllPersonne();
		Assertions.assertNotNull(personneList);
		Assertions.assertTrue(CollectionUtils.isNotEmpty(personneList));
        Assertions.assertEquals(4, personneList.size(), "personneList.size() should be 4 but is " + personneList.size());
		for (Personne personne : personneList) {
			Personne p = personneService.findByPersonneId(personne.getId());
			Assertions.assertNotNull(p);
		}
		Film film2 = new FilmBuilder.Builder(FilmBuilder.TITRE_FILM_TMBD_ID_4780)
				.setTitreO(FilmBuilder.TITRE_FILM_TMBD_ID_4780)
				.setAct1Nom(FilmBuilder.ACT1_TMBD_ID_844)
				.setAct2Nom(FilmBuilder.ACT2_TMBD_ID_844)
				.setAct3Nom(FilmBuilder.ACT3_TMBD_ID_844)
				.setRipped(true)
				.setAnnee(FilmBuilder.ANNEE)
				.setDateSortie(FilmBuilder.FILM_DATE_SORTIE).setDateInsertion(FilmBuilder.FILM_DATE_INSERTION)
				.setDvdFormat(DvdFormat.DVD)
				.setOrigine(FilmOrigine.DVD)
				.setGenre1(genre1)
				.setGenre2(genre2)
				.setZone(2)
				.setRealNom(FilmBuilder.REAL_NOM_TMBD_ID_844)
				.setRipDate(FilmBuilder.createRipDate(FilmBuilder.RIP_DATE_OFFSET)).setDateSortieDvd(FilmBuilder.DVD_DATE_SORTIE)
				.setAllocineFicheFilmId(FilmBuilder.ALLOCINE_FICHE_FILM_ID_844).build();
		Long filmId2 = filmService.saveNewFilm(film2);
		Assertions.assertNotNull(filmId2);
		FilmBuilder.assertFilmIsNotNull(film2, false,FilmBuilder.RIP_DATE_OFFSET, FilmOrigine.DVD, null, null, false);
		List<Personne> personne2List = personneService.findAllPersonne();
		Assertions.assertNotNull(personne2List);
		Assertions.assertTrue(CollectionUtils.isNotEmpty(personne2List));
        Assertions.assertEquals(4, personne2List.size());
		for (Personne personne : personne2List) {
			Personne p = personneService.findByPersonneId(personne.getId());
			Assertions.assertNotNull(p);
		}
	}
	@Test
	public void findPersonneByFullName() throws Exception {
		Genre genre1 = filmService.saveGenre(new Genre(28,"Action"));
		Genre genre2 = filmService.saveGenre(new Genre(35,"Comedy"));
		Film film = new FilmBuilder.Builder(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setTitreO(FilmBuilder.TITRE_FILM_TMBD_ID_844)
				.setAct1Nom(FilmBuilder.ACT1_TMBD_ID_844)
				.setAct2Nom(FilmBuilder.ACT2_TMBD_ID_844)
				.setAct3Nom(FilmBuilder.ACT3_TMBD_ID_844)
				.setRipped(true)
				.setAnnee(FilmBuilder.ANNEE)
				.setDateSortie(FilmBuilder.FILM_DATE_SORTIE).setDateInsertion(FilmBuilder.FILM_DATE_INSERTION)
				.setDvdFormat(DvdFormat.DVD)
				.setOrigine(FilmOrigine.DVD)
				.setGenre1(genre1)
				.setGenre2(genre2)
				.setZone(2)
				.setRealNom(FilmBuilder.REAL_NOM_TMBD_ID_844)
				.setRipDate(FilmBuilder.createRipDate(FilmBuilder.RIP_DATE_OFFSET)).setDateSortieDvd(FilmBuilder.DVD_DATE_SORTIE)
				.setAllocineFicheFilmId(FilmBuilder.ALLOCINE_FICHE_FILM_ID_844).build();
		Long filmId = filmService.saveNewFilm(film);
		Assertions.assertNotNull(filmId);
		FilmBuilder.assertFilmIsNotNull(film, false,FilmBuilder.RIP_DATE_OFFSET, FilmOrigine.DVD, null, null, false);
		Personne personne = personneService.findPersonneByName(FilmBuilder.ACT1_TMBD_ID_844);
		Assertions.assertNotNull(personne);
	}
	
	@Test
	public void cleanAllPersonne() {
		String methodName = "cleanAllPersonne : ";
		logger.debug(methodName + "start");
		personneService.cleanAllPersonnes();
		logger.debug(methodName + "end");
	}
	@Test
	@Disabled
	public void deletePersonne() {
		String methodName = "deletePersonne : ";
		logger.debug(methodName + "start");
		Long maxPersonneId = this.jdbcTemplate.queryForObject("select max(ID) from Personne", Long.class);
		// PersonneDto personneDto = personneService.findByPersonneId(maxPersonneId);
		PersonneDto personneDto = new PersonneDto();
		personneDto.setId(maxPersonneId);
		//personneService.deletePersonne(personneDto);
		logger.debug(methodName + "end");
	}
}
