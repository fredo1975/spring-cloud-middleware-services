package fr.bluechipit.dvdtheque.allocine.dto;

import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FicheFilmRecTest {

    @Test
    @DisplayName("fromEntity returns null for a null entity")
    void fromEntity_NullEntity_ReturnsNull() {
        assertThat(FicheFilmRec.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("fromEntity returns null for an unsaved entity (id == 0)")
    void fromEntity_UnsavedEntity_ReturnsNull() {
        FicheFilm film = new FicheFilm("Inception", 123, "url", 1);

        assertThat(FicheFilmRec.fromEntity(film)).isNull();
    }

    @Test
    @DisplayName("fromEntity maps every field and the critiques")
    void fromEntity_MapsAllFieldsAndCritiques() {
        FicheFilm film = new FicheFilm("Inception", 123, "https://www.allocine.fr/film/fichefilm-123/", 2);
        film.setId(42);
        film.setCritiquePresse(new java.util.HashSet<>());
        fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse critique =
                new fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse();
        critique.setNewsSource("Le Figaro");
        critique.setAuthor("Jean Dupont");
        critique.setBody("Excellent");
        critique.setRating(4.0);
        critique.setFicheFilm(film);
        film.addCritiquePresse(critique);

        FicheFilmRec rec = FicheFilmRec.fromEntity(film);

        assertThat(rec).isNotNull();
        assertThat(rec.id()).isEqualTo(42);
        assertThat(rec.allocineFilmId()).isEqualTo(123);
        assertThat(rec.url()).isEqualTo("https://www.allocine.fr/film/fichefilm-123/");
        assertThat(rec.pageNumber()).isEqualTo(2);
        assertThat(rec.title()).isEqualTo("Inception");
        assertThat(rec.creationDate()).isEqualTo(film.getCreationDate());
        assertThat(rec.critiquePresse()).hasSize(1);
        CritiquePresseRec critiqueRec = rec.critiquePresse().iterator().next();
        assertThat(critiqueRec.newsSource()).isEqualTo("Le Figaro");
        assertThat(critiqueRec.author()).isEqualTo("Jean Dupont");
        assertThat(critiqueRec.body()).isEqualTo("Excellent");
        assertThat(critiqueRec.rating()).isEqualTo(4.0);
    }
}
