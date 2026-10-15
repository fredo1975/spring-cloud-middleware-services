package fr.bluechipit.dvdtheque.dao.domain;

import fr.bluechipit.dvdtheque.model.CritiquePresse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Film (entité domaine) Unit Tests")
class FilmTest {

    @Test
    @DisplayName("addCritiquePresse() doit initialiser la liste si elle est nulle")
    void addCritiquePresse_ShouldLazilyInitializeList() {
        Film film = new Film();
        assertThat(film.getCritiquePresse()).isNull();

        film.addCritiquePresse(new CritiquePresse());

        assertThat(film.getCritiquePresse()).hasSize(1);
    }

    @Test
    @DisplayName("addCritiquePresse() doit ajouter aux critiques existantes")
    void addCritiquePresse_ShouldAppendToExistingList() {
        Film film = new Film();
        film.addCritiquePresse(new CritiquePresse());
        film.addCritiquePresse(new CritiquePresse());

        assertThat(film.getCritiquePresse()).hasSize(2);
    }

    @Test
    @DisplayName("compareTo() doit comparer par titre")
    void compareTo_ShouldCompareByTitre() {
        Film a = new Film();
        a.setTitre("A");
        Film b = new Film();
        b.setTitre("B");

        assertThat(a.compareTo(b)).isNegative();
        assertThat(b.compareTo(a)).isPositive();
    }

    @Test
    @DisplayName("equals()/hashCode() reposent sur l'id")
    void equalsAndHashCode_ShouldUseId() {
        Film a = new Film(1L);
        Film b = new Film(1L);
        Film c = new Film(2L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("equals() avec deux ids nuls est vrai")
    void equals_WithNullIdsAreEqual() {
        assertThat(new Film()).isEqualTo(new Film());
    }
}
