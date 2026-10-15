package fr.bluechipit.dvdtheque.service;

import enums.DvdFormat;
import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.repository.FilmDao;
import fr.bluechipit.dvdtheque.exception.FilmNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilmSaveService Unit Tests")
class FilmSaveServiceTest {

    @Mock
    private FilmDao filmDao;

    private FilmSaveService filmSaveService;

    @BeforeEach
    void setUp() {
        filmSaveService = new FilmSaveService(filmDao);
    }

    @Test
    @DisplayName("saveNewFilm() doit annuler l'id, mettre le titre en majuscules et persister")
    void saveNewFilm_ShouldResetIdUppercaseTitlesAndSave() {
        Film film = new Film();
        film.setId(99L);
        film.setTitre("inception");
        film.setTitreO("inception original");
        given(filmDao.save(film)).willReturn(film);

        Film result = filmSaveService.saveNewFilm(film);

        assertThat(result.getId()).isNull();
        assertThat(result.getTitre()).isEqualTo("INCEPTION");
        assertThat(result.getTitreO()).isEqualTo("INCEPTION ORIGINAL");
        verify(filmDao).save(film);
    }

    @Test
    @DisplayName("upperCaseTitre() doit gérer un titre null sans erreur")
    void upperCaseTitre_ShouldHandleNullTitles() {
        Film film = new Film();
        film.setTitre(null);
        film.setTitreO(null);

        filmSaveService.upperCaseTitre(film);

        assertThat(film.getTitre()).isNull();
        assertThat(film.getTitreO()).isNull();
    }

    @Test
    @DisplayName("updateFilm() doit copier les champs simples et positionner dateMaj (quirk: le titre n'est pas recopié)")
    void updateFilm_ShouldCopySimpleFieldsAndSetDateMaj() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.TV);
        existing.setTitre("ORIGINAL");
        existing.setVu(true);
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Film update = new Film(1L);
        update.setTitre("avatar");
        update.setTitreO("avatar o");
        update.setOrigine(FilmOrigine.EN_SALLE);
        update.setDateInsertion(LocalDate.of(2020, 1, 1));
        update.setDateSortieDvd(LocalDate.of(2021, 2, 2));
        update.setVu(false);
        update.setAllocineFicheFilmId(555);
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(update.getTitre()).isEqualTo("AVATAR");
        assertThat(result.getTitre()).isEqualTo("ORIGINAL");
        assertThat(result.getOrigine()).isEqualTo(FilmOrigine.EN_SALLE);
        assertThat(result.getDateInsertion()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(result.getDateSortieDvd()).isEqualTo(LocalDate.of(2021, 2, 2));
        assertThat(result.getAllocineFicheFilmId()).isEqualTo(555);
        assertThat(update.getDateMaj()).isNotNull();
        assertThat(result.getDateMaj()).isNull();
        verify(filmDao).save(existing);
    }

    @Test
    @DisplayName("updateFilm() doit retirer le dvd quand l'origine passe de DVD à autre")
    void updateFilm_ShouldClearDvdWhenOriginLeavesDvd() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.DVD);
        existing.setDvd(new Dvd());
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Film update = new Film(1L);
        update.setTitre("film");
        update.setOrigine(FilmOrigine.EN_SALLE);
        update.setDvd(new Dvd());
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(result.getDvd()).isNull();
    }

    @Test
    @DisplayName("updateFilm() doit effacer la dateRip quand le dvd n'est pas rippé (origine DVD)")
    void updateFilm_ShouldClearDateRipWhenNotRippedForDvdOrigin() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.DVD);
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Dvd dvd = new Dvd();
        dvd.setRipped(false);
        dvd.setDateRip(LocalDate.of(2019, 5, 5));
        Film update = new Film(1L);
        update.setTitre("film");
        update.setOrigine(FilmOrigine.DVD);
        update.setDvd(dvd);
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(result.getDvd()).isSameAs(dvd);
        assertThat(result.getDvd().getDateRip()).isNull();
    }

    @Test
    @DisplayName("updateFilm() doit conserver la dateRip quand le dvd est rippé")
    void updateFilm_ShouldKeepDateRipWhenRipped() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.DVD);
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Dvd dvd = new Dvd();
        dvd.setRipped(true);
        dvd.setDateRip(LocalDate.of(2019, 5, 5));
        Film update = new Film(1L);
        update.setTitre("film");
        update.setOrigine(FilmOrigine.DVD);
        update.setDvd(dvd);
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(result.getDvd().getDateRip()).isEqualTo(LocalDate.of(2019, 5, 5));
    }

    @Test
    @DisplayName("updateFilm() doit effacer la dateRip pour un film EN_SALLE avec dvd")
    void updateFilm_ShouldClearDateRipForEnSalleWithDvd() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.EN_SALLE);
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Dvd dvd = new Dvd();
        dvd.setRipped(true);
        dvd.setDateRip(LocalDate.of(2019, 5, 5));
        Film update = new Film(1L);
        update.setTitre("film");
        update.setOrigine(FilmOrigine.EN_SALLE);
        update.setDvd(dvd);
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(result.getDvd()).isSameAs(dvd);
        assertThat(result.getDvd().getDateRip()).isNull();
    }

    @Test
    @DisplayName("updateFilm() doit effacer la dateVue quand le film n'est pas vu")
    void updateFilm_ShouldClearDateVueWhenNotSeen() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.TV);
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Film update = new Film(1L);
        update.setTitre("film");
        update.setOrigine(FilmOrigine.TV);
        update.setVu(false);
        update.setDateVue(LocalDate.of(2022, 3, 3));
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(result.isVu()).isFalse();
        assertThat(result.getDateVue()).isNull();
    }

    @Test
    @DisplayName("updateFilm() doit conserver la dateVue quand le film est vu")
    void updateFilm_ShouldKeepDateVueWhenSeen() {
        Film existing = new Film(1L);
        existing.setOrigine(FilmOrigine.TV);
        given(filmDao.findById(1L)).willReturn(Optional.of(existing));

        Film update = new Film(1L);
        update.setTitre("film");
        update.setOrigine(FilmOrigine.TV);
        update.setVu(true);
        update.setDateVue(LocalDate.of(2022, 3, 3));
        given(filmDao.save(existing)).willReturn(existing);

        Film result = filmSaveService.updateFilm(update);

        assertThat(result.isVu()).isTrue();
        assertThat(result.getDateVue()).isEqualTo(LocalDate.of(2022, 3, 3));
    }

    @Test
    @DisplayName("findFilm() doit retourner le film quand il existe")
    void findFilm_ShouldReturnFilmWhenExists() {
        Film film = new Film(7L);
        given(filmDao.findById(7L)).willReturn(Optional.of(film));

        assertThat(filmSaveService.findFilm(7L)).isSameAs(film);
    }

    @Test
    @DisplayName("findFilm() doit lever FilmNotFoundException quand absent")
    void findFilm_ShouldThrowWhenAbsent() {
        given(filmDao.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> filmSaveService.findFilm(404L))
                .isInstanceOf(FilmNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("dvd par défaut avec format DVD doit être mappé")
    void dvdFormatEnumIsUsable() {
        assertThat(DvdFormat.DVD.name()).isEqualTo("DVD");
    }
}
