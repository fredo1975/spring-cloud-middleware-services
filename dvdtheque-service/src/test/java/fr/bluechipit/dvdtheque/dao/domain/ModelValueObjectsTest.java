package fr.bluechipit.dvdtheque.dao.domain;

import enums.DvdFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dvd / Genre / Pays (entités domaine) Unit Tests")
class ModelValueObjectsTest {

    @Test
    @DisplayName("Dvd doit exposer ses accesseurs et equals/hashCode par id")
    void dvd_ShouldExposeAccessorsAndIdEquality() {
        Dvd dvd = new Dvd();
        dvd.setId(1L);
        dvd.setAnnee(2010);
        dvd.setZone(2);
        dvd.setEdition("Collector");
        dvd.setDateRip(LocalDate.of(2020, 1, 1));
        dvd.setFormat(DvdFormat.DVD);
        dvd.setRipped(true);

        assertThat(dvd.getId()).isEqualTo(1L);
        assertThat(dvd.getAnnee()).isEqualTo(2010);
        assertThat(dvd.getZone()).isEqualTo(2);
        assertThat(dvd.getEdition()).isEqualTo("Collector");
        assertThat(dvd.getDateRip()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(dvd.getFormat()).isEqualTo(DvdFormat.DVD);
        assertThat(dvd.isRipped()).isTrue();
        assertThat(dvd).isEqualTo(dvd);

        Dvd same = new Dvd();
        same.setId(1L);
        assertThat(dvd).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(dvd).isNotEqualTo(new Dvd());
    }

    @Test
    @DisplayName("Genre doit être constructible par (tmdbId, name) et comparable par id")
    void genre_ShouldSupportConstructorAndIdEquality() {
        Genre genre = new Genre(28, "Action");
        genre.setId(5L);

        assertThat(genre.getTmdbId()).isEqualTo(28);
        assertThat(genre.getName()).isEqualTo("Action");

        Genre same = new Genre(99, "Other");
        same.setId(5L);
        assertThat(genre).isEqualTo(same);

        Genre different = new Genre(28, "Action");
        different.setId(6L);
        assertThat(genre).isNotEqualTo(different);
    }

    @Test
    @DisplayName("Pays doit être un simple POJO (annotations JPA commentées)")
    void pays_ShouldBehaveAsPlainPojo() {
        Pays pays = new Pays();
        pays.setId(1);
        pays.setLib("France");
        pays.setI18n("FR");

        assertThat(pays.getId()).isEqualTo(1);
        assertThat(pays.getLib()).isEqualTo("France");
        assertThat(pays.getI18n()).isEqualTo("FR");
        assertThat(pays).isEqualTo(pays);

        Pays same = new Pays();
        same.setId(1);
        assertThat(pays).isEqualTo(same).hasSameHashCodeAs(same);
    }
}
