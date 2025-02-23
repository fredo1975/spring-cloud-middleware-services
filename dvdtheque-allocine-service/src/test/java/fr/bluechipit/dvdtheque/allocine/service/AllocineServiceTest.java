package fr.bluechipit.dvdtheque.allocine.service;

import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.repository.FicheFilmRepository;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class AllocineServiceTest {
	@Autowired
	private FicheFilmRepository ficheFilmRepository;

	@Test
	public void testRemoveFilm() {
		FicheFilm ficheFilmSaved = saveFilm();
		final String title = ficheFilmSaved.getTitle();
		ficheFilmRepository.delete(ficheFilmSaved);
		List<FicheFilm> ficheFilmRetrieved = ficheFilmRepository.findByTitle(title);
		assertTrue(CollectionUtils.isEmpty(ficheFilmRetrieved));
	}
	private FicheFilm saveFilm() {
		FicheFilm ficheFilm = new FicheFilm("title",1,"url",1);
		CritiquePresse cp = new CritiquePresse();
		cp.setAuthor("author1");
		cp.setBody("body1");
		cp.setNewsSource("source1");
		cp.setRating(4d);
		cp.setFicheFilm(ficheFilm);
		ficheFilm.addCritiquePresse(cp);
		FicheFilm ficheFilmSaved = ficheFilmRepository.save(ficheFilm);
		Assertions.assertNotNull(ficheFilmSaved);
		return ficheFilmSaved;
	}
	@Test
	public void testFindByTitle() {
		FicheFilm ficheFilmSaved = saveFilm();
		Assertions.assertNotNull(ficheFilmSaved);
		List<FicheFilm> ficheFilmRetrieved = ficheFilmRepository.findByTitle("title");
		Assertions.assertNotNull(ficheFilmRetrieved);
		Assertions.assertNotNull(ficheFilmRetrieved.get(0));
		Assertions.assertNotNull(ficheFilmRetrieved.get(0).getCreationDate());
		Assertions.assertNotNull(ficheFilmRetrieved.get(0).getCritiquePresse());
        assertEquals("source1", ficheFilmRetrieved.get(0).getCritiquePresse().iterator().next().getNewsSource());
		System.out.println(ficheFilmRetrieved);
	}
	@Test
	public void testFindById() {
		FicheFilm ficheFilmSaved = saveFilm();
		Assertions.assertNotNull(ficheFilmSaved);
		Optional<FicheFilm> ficheFilmRetrieved = ficheFilmRepository.findById(ficheFilmSaved.getId());
		assertThat(ficheFilmRetrieved).isPresent();
		assertThat(ficheFilmRetrieved.get().getCreationDate()).isNotNull();
		assertThat(ficheFilmRetrieved.get().getCritiquePresse()).isNotNull();
		assertThat(ficheFilmRetrieved.get().getCritiquePresse().iterator().next().getNewsSource()).isEqualTo("source1");
		System.out.println(ficheFilmRetrieved);
	}
}
