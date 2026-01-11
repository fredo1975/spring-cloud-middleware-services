package fr.bluechipit.dvdtheque.service.impl;

import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.repository.FilmDao;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class FilmSaveService {
    private static final String REALISATEUR_MESSAGE_WARNING = "Film should contains one producer";
    private final FilmDao filmDao;
    public FilmSaveService(FilmDao filmDao) {
        this.filmDao = filmDao;
    }
    @Transactional(readOnly = false)
    public Long saveNewFilm(Film film) {
        Assert.notEmpty(film.getRealisateur(), REALISATEUR_MESSAGE_WARNING);
        upperCaseTitre(film);
        Film savedFilm = filmDao.save(film);
        return savedFilm.getId();
    }

    public void upperCaseTitre(final Film film) {
        final String titre = StringUtils.upperCase(film.getTitre());
        film.setTitre(titre);
        final String titreO = StringUtils.upperCase(film.getTitreO());
        film.setTitreO(titreO);
    }

}
