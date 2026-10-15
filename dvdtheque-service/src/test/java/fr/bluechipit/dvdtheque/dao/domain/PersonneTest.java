package fr.bluechipit.dvdtheque.dao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Personne (entité domaine) Unit Tests")
class PersonneTest {

    @Test
    @DisplayName("equals()/hashCode() reposent sur le nom")
    void equalsAndHashCode_ShouldUseNom() {
        Personne a = new Personne();
        a.setNom("NOLAN");
        Personne b = new Personne();
        b.setNom("NOLAN");
        b.setId(99L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("equals() doit gérer un nom nul")
    void equals_ShouldHandleNullNom() {
        Personne a = new Personne();
        Personne b = new Personne();

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("compareTo() doit comparer par nom")
    void compareTo_ShouldCompareByNom() {
        Personne a = new Personne();
        a.setNom("A");
        Personne b = new Personne();
        b.setNom("B");

        assertThat(a.compareTo(b)).isNegative();
    }
}
