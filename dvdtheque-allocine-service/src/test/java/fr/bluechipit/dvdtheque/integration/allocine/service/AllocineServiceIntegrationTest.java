package fr.bluechipit.dvdtheque.integration.allocine.service;

import fr.bluechipit.dvdtheque.allocine.AllocineServiceApplication;
import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.service.AllocineService;
import fr.bluechipit.dvdtheque.integration.allocine.config.HazelcastConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {HazelcastConfiguration.class, AllocineServiceApplication.class})
@ActiveProfiles("test")
public class AllocineServiceIntegrationTest {
	@MockBean
	JwtDecoder jwtDecoder;
	protected Logger logger = LoggerFactory.getLogger(AllocineServiceIntegrationTest.class);
	@Autowired
	private AllocineService allocineService;
	
	private FicheFilm saveFilm() {
		FicheFilm ficheFilm = new FicheFilm("title",1555,"url",1);
		CritiquePresse cp = new CritiquePresse();
		cp.setAuthor("author1");
		cp.setBody("body1");
		cp.setNewsSource("source1");
		cp.setRating(4d);
		cp.setFicheFilm(ficheFilm);
		ficheFilm.addCritiquePresse(cp);
		FicheFilm ficheFilmSaved = allocineService.saveFicheFilm(ficheFilm);
		assertNotNull(ficheFilmSaved);
		return ficheFilmSaved;
	}
	
    @Test
    @Transactional
    //@Disabled
    public void retrieveAllocineScrapingFicheFilmTest() throws IOException {
    	/*
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("SCOPE_read");
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);
    		*/
    	allocineService.scrapAllAllocineFicheFilm();
    	TestTransaction.flagForCommit();
		List<FicheFilm> allFicheFilmFromPageRetrievedFromDb = allocineService.retrieveAllFicheFilm();
		//assertEquals(28,allFicheFilmFromPageRetrievedFromDb.size());
		assertTrue(allFicheFilmFromPageRetrievedFromDb.size()>10);
		FicheFilm ficheFilm0 = allFicheFilmFromPageRetrievedFromDb.get(0);
		Optional<FicheFilm> optionalFicheFilmRetrievedFromCache = allocineService.findInCacheByFicheFilmId(ficheFilm0.getAllocineFilmId());
		assertEquals(optionalFicheFilmRetrievedFromCache.get(),ficheFilm0);
		
		Optional<FicheFilm> optionalFicheFilmRetrievedFromCache2 = allocineService.findInCacheByFicheFilmId(allFicheFilmFromPageRetrievedFromDb.get(allFicheFilmFromPageRetrievedFromDb.size()-1).getAllocineFilmId());
		assertEquals(optionalFicheFilmRetrievedFromCache2.get(),allFicheFilmFromPageRetrievedFromDb.get(allFicheFilmFromPageRetrievedFromDb.size()-1));
		List<FicheFilm> ficheFilmListDbRetrieved0 = allocineService.retrieveFicheFilmByTitle(ficheFilm0.getTitle());
		assertNotNull(ficheFilmListDbRetrieved0);
		Optional<List<FicheFilm>> ficheFilmCacheRetrievd0 = allocineService.findInCacheByFicheFilmTitle(ficheFilmListDbRetrieved0.get(0).getTitle());
		assertEquals(ficheFilmCacheRetrievd0.get().get(0),ficheFilm0);
    }
    
    @Test
	public void paginatedSarch() throws ParseException{
		FicheFilm ficheFilmSaved = saveFilm();
		var page = allocineService.paginatedSarch("", 1, 1, "-title");
		assertNotNull(page);
		assertThat(page.getContent()).isNotEmpty();
		assertThat(page.getContent().size()==1).isTrue();
		var it = page.getContent().iterator();
		assertNotNull(it);
		var f = it.next();
		assertNotNull(f);
		assertThat(ficheFilmSaved.getTitle()).isEqualTo(f.title());
		var query = "title:eq:it:AND";
		page = allocineService.paginatedSarch(query, 1, 1, "-title");
		assertNotNull(page);
		assertThat(page.getContent()).isNotEmpty();
		assertThat(page.getContent().size()==1).isTrue();
		it = page.getContent().iterator();
		assertNotNull(it);
		f = it.next();
		assertNotNull(f);
		assertThat(ficheFilmSaved.getTitle()).isEqualTo(f.title());
	}
}
