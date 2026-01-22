package fr.bluechipit.dvdtheque.service.impl;

import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.repository.FilmDao;
import fr.bluechipit.dvdtheque.exception.FilmNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Service
public class FilmSaveService {
    private static final String REALISATEUR_MESSAGE_WARNING = "Film should contains one producer";
    private final FilmDao filmDao;
    public FilmSaveService(FilmDao filmDao) {
        this.filmDao = filmDao;
    }
    @Transactional
    public Film saveNewFilm(Film film) {
        Assert.notEmpty(film.getRealisateur(), REALISATEUR_MESSAGE_WARNING);
        // Ensure we aren't accidentally updating an existing record
        film.setId(null);
        upperCaseTitre(film);
        // Spring Data JPA returns the persisted entity with the generated ID
        return filmDao.save(film);
    }

    public void upperCaseTitre(final Film film) {
        final String titre = StringUtils.upperCase(film.getTitre());
        film.setTitre(titre);
        final String titreO = StringUtils.upperCase(film.getTitreO());
        film.setTitreO(titreO);
    }
    @Transactional
    public Film updateFilm(Film film) {
        upperCaseTitre(film);
        film.setDateMaj(LocalDateTime.now());
        var filmRetrieved = findFilm(film.getId());
        if(filmRetrieved.getOrigine() == FilmOrigine.DVD && film.getOrigine() != FilmOrigine.DVD) {
            filmRetrieved.setDvd(null);
        }
        if (film.getDvd() != null && film.getOrigine() == FilmOrigine.DVD) {
            filmRetrieved.setDvd(film.getDvd());
            if (!film.getDvd().isRipped()) {
                filmRetrieved.getDvd().setDateRip(null);
            }
        }
        if(film.getDvd() != null && filmRetrieved.getOrigine() == FilmOrigine.EN_SALLE) {
            filmRetrieved.setDvd(film.getDvd());
            filmRetrieved.getDvd().setDateRip(null);
        }
        filmRetrieved.setOrigine(film.getOrigine());
        filmRetrieved.setDateInsertion(film.getDateInsertion());
        filmRetrieved.setDateSortieDvd(film.getDateSortieDvd());
        filmRetrieved.setVu(film.isVu());
        if(!filmRetrieved.isVu()) {
            filmRetrieved.setDateVue(null);
        }else {
            filmRetrieved.setDateVue(film.getDateVue());
        }
        filmRetrieved.setAllocineFicheFilmId(film.getAllocineFicheFilmId());
        return filmDao.save(filmRetrieved);
    }
    @Transactional(readOnly = true)
    public Film findFilm(final Long id) {
        return filmDao.findById(id)
                .orElseThrow(()->new FilmNotFoundException(String.format("film with id %s not found", id)));
    }

}
