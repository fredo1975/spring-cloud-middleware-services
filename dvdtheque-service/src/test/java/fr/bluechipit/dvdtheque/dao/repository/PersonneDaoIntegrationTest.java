package fr.bluechipit.dvdtheque.dao.repository;


import fr.bluechipit.dvdtheque.dao.domain.Personne;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.Assert.assertNotNull;

@ExtendWith(SpringExtension.class)
//@Import(PersonneDaoImpl.class)
@DataJpaTest
@ActiveProfiles("test")
public class PersonneDaoIntegrationTest {
	protected Logger logger = LoggerFactory.getLogger(PersonneDaoIntegrationTest.class);
	@Autowired
    private PersonneDao personneDao;
	@Test
	public void findAllRealisateur(){
		List<Personne> realisateurs = personneDao.findAll();
		Assertions.assertNotNull(realisateurs);
		logger.info("realisateurs.size()="+realisateurs.size());
	}
	@Test
	public void findAllActeur(){
		List<Personne> acteurs = personneDao.findAll();
		Assertions.assertNotNull(acteurs);
		logger.info("acteurs.size()="+acteurs.size());
	}
	@Test
	public void findAllPersonne(){
		List<Personne> personnes = personneDao.findAll();
		Assertions.assertNotNull(personnes);
		logger.info("personnes.size()="+personnes.size());
	}
}
