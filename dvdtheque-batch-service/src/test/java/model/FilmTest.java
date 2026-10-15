package model;

import enums.FilmOrigine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class FilmTest {

    @Test
    @DisplayName("equality and hashCode are based on the identifier")
    void equalsAndHashCode_ShouldUseId() {
        Film a = new Film(5L);
        Film b = new Film(5L);
        Film c = new Film(6L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a film");
    }

    @Test
    @DisplayName("films with a null id are equal to each other only")
    void equals_WithNullId() {
        assertThat(new Film()).isEqualTo(new Film());
        assertThat(new Film()).isNotEqualTo(new Film(1L));
    }

    @Test
    @DisplayName("compareTo orders by title")
    void compareTo_ShouldOrderByTitle() {
        Film a = new Film(1L);
        a.setTitre("Alpha");
        Film b = new Film(2L);
        b.setTitre("Beta");

        assertThat(a.compareTo(b)).isNegative();
        assertThat(b.compareTo(a)).isPositive();
        assertThat(a.compareTo(a)).isZero();
    }

    @Test
    @DisplayName("accessors round trip every field")
    void accessors_ShouldRoundTrip() {
        Film film = new Film(1L);
        Set<Personne> realisateur = new HashSet<>();
        Set<Personne> acteur = new HashSet<>();
        Set<CritiquesPresse> critiques = new TreeSet<>();
        Set<Genre> genre = new HashSet<>();
        Dvd dvd = new Dvd();

        film.setAnnee(2012);
        film.setDateSortie(LocalDate.of(2012, 1, 1));
        film.setDateInsertion(LocalDate.of(2013, 1, 1));
        film.setDateSortieDvd(LocalDate.of(2013, 2, 1));
        film.setDateVue(LocalDate.of(2013, 3, 1));
        film.setTitre("titre");
        film.setTitreO("title");
        film.setDvd(dvd);
        film.setOrigine(FilmOrigine.DVD);
        film.setRealisateur(realisateur);
        film.setActeur(acteur);
        film.setCritiquesPresse(critiques);
        film.setVu(true);
        film.setPosterPath("/poster");
        film.setTmdbId(42L);
        film.setOverview("overview");
        film.setRuntime(120);
        film.setGenre(genre);
        film.setHomepage("homepage");
        film.setAlreadyInDvdtheque(true);

        assertThat(film.getId()).isEqualTo(1L);
        assertThat(film.getAnnee()).isEqualTo(2012);
        assertThat(film.getDateSortie()).isEqualTo(LocalDate.of(2012, 1, 1));
        assertThat(film.getDateInsertion()).isEqualTo(LocalDate.of(2013, 1, 1));
        assertThat(film.getDateSortieDvd()).isEqualTo(LocalDate.of(2013, 2, 1));
        assertThat(film.getDateVue()).isEqualTo(LocalDate.of(2013, 3, 1));
        assertThat(film.getTitre()).isEqualTo("titre");
        assertThat(film.getTitreO()).isEqualTo("title");
        assertThat(film.getDvd()).isSameAs(dvd);
        assertThat(film.getOrigine()).isEqualTo(FilmOrigine.DVD);
        assertThat(film.getRealisateur()).isSameAs(realisateur);
        assertThat(film.getActeur()).isSameAs(acteur);
        assertThat(film.getCritiquesPresse()).isSameAs(critiques);
        assertThat(film.isVu()).isTrue();
        assertThat(film.getPosterPath()).isEqualTo("/poster");
        assertThat(film.getTmdbId()).isEqualTo(42L);
        assertThat(film.getOverview()).isEqualTo("overview");
        assertThat(film.getRuntime()).isEqualTo(120);
        assertThat(film.getGenre()).isSameAs(genre);
        assertThat(film.getHomepage()).isEqualTo("homepage");
        assertThat(film.isAlreadyInDvdtheque()).isTrue();
    }

    @Test
    @DisplayName("toString contains the key fields")
    void toString_ShouldContainFields() {
        Film film = new Film(1L);
        film.setTitre("titre");

        assertThat(film.toString()).contains("id=1", "titre=titre");
    }
}
