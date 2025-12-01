package fr.bluechipit.dvdtheque.allocine.service;

import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.dto.FicheFilmDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AllocineService {
	void scrapAllAllocineFicheFilm();
	List<FicheFilm> retrieveAllFicheFilm();
	Optional<FicheFilm> retrieveFicheFilm(int id);
	List<FicheFilm> retrieveFicheFilmByTitle(String title);
	Optional<FicheFilm> findByFicheFilmId(Integer ficheFilmId);
	FicheFilm saveFicheFilm(FicheFilm ficheFilm);
	List<FicheFilm> saveFicheFilmList(List<FicheFilm> ficheFilmList);
	Optional<FicheFilm> findInCacheByFicheFilmId(Integer ficheFilmId);
	Optional<List<FicheFilm>> findInCacheByFicheFilmTitle(String title);
	Page<FicheFilmDto> paginatedSarch(String query, Integer offset, Integer limit, String sort);
	/**
	 * 
	 */
	void scrapAllAllocineFicheFilmMultithreaded();
}
