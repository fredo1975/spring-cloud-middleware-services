package model;

import enums.DvdFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ModelValueObjectsTest {

    @Test
    @DisplayName("Genre equality and hashCode use the identifier")
    void genre_EqualsAndHashCode() {
        Genre a = new Genre();
        a.setId(1L);
        Genre b = new Genre();
        b.setId(1L);
        Genre c = new Genre();
        c.setId(2L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(new Genre()).isEqualTo(new Genre());
    }

    @Test
    @DisplayName("Genre constructor and accessors cover the fields")
    void genre_Accessors() {
        Genre genre = new Genre(28, "Action");
        genre.setId(1L);

        assertThat(genre.getId()).isEqualTo(1L);
        assertThat(genre.getTmdbId()).isEqualTo(28);
        assertThat(genre.getName()).isEqualTo("Action");
        assertThat(genre.toString()).contains("id=1", "name=Action", "tmdbId=28");
    }

    @Test
    @DisplayName("Pays equality and hashCode use the identifier")
    void pays_EqualsAndHashCode() {
        Pays a = new Pays();
        a.setId(1);
        Pays b = new Pays();
        b.setId(1);
        Pays c = new Pays();
        c.setId(2);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(new Pays()).isEqualTo(new Pays());
    }

    @Test
    @DisplayName("Pays accessors and toString cover the fields")
    void pays_Accessors() {
        Pays pays = new Pays();
        pays.setLib("France");
        pays.setI18n("FR");

        assertThat(pays.getLib()).isEqualTo("France");
        assertThat(pays.getI18n()).isEqualTo("FR");
        assertThat(pays.toString()).contains("lib=France", "i18n=FR");
    }

    @Test
    @DisplayName("Dvd equality and hashCode use the identifier")
    void dvd_EqualsAndHashCode() {
        Dvd a = new Dvd();
        a.setId(1L);
        Dvd b = new Dvd();
        b.setId(1L);
        Dvd c = new Dvd();
        c.setId(2L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(new Dvd()).isEqualTo(new Dvd());
    }

    @Test
    @DisplayName("Dvd accessors and toString cover the fields")
    void dvd_Accessors() {
        Dvd dvd = new Dvd();
        dvd.setAnnee(2012);
        dvd.setZone(2);
        dvd.setEdition("Collector");
        dvd.setDateRip(LocalDate.of(2013, 8, 1));
        dvd.setFormat(DvdFormat.DVD);
        dvd.setRipped(true);

        assertThat(dvd.getAnnee()).isEqualTo(2012);
        assertThat(dvd.getZone()).isEqualTo(2);
        assertThat(dvd.getEdition()).isEqualTo("Collector");
        assertThat(dvd.getDateRip()).isEqualTo(LocalDate.of(2013, 8, 1));
        assertThat(dvd.getFormat()).isEqualTo(DvdFormat.DVD);
        assertThat(dvd.isRipped()).isTrue();
        assertThat(dvd.toString()).contains("annee=2012", "edition=Collector", "ripped=true");
    }

    @Test
    @DisplayName("DvdBuilder accessors and toString cover the fields")
    void dvdBuilder_Accessors() {
        DvdBuilder builder = new DvdBuilder();
        Film film = new Film(1L);
        builder.setFilmToSave(film);
        builder.setZonedvd(2);
        builder.setFilmFormat("DVD");

        assertThat(builder.getFilmToSave()).isSameAs(film);
        assertThat(builder.getZonedvd()).isEqualTo(2);
        assertThat(builder.getFilmFormat()).isEqualTo("DVD");
        assertThat(builder.toString()).contains("zonedvd=2", "filmFormat=DVD");
    }
}
