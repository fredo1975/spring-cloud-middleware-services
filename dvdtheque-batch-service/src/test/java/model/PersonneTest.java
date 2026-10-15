package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class PersonneTest {

    private Personne personne(Long id, String nom) {
        Personne personne = new Personne();
        personne.setId(id);
        personne.setNom(nom);
        return personne;
    }

    @Test
    @DisplayName("compareTo orders by name")
    void compareTo_ShouldOrderByName() {
        assertThat(personne(1L, "Alice").compareTo(personne(2L, "Bob"))).isNegative();
        assertThat(personne(2L, "Bob").compareTo(personne(1L, "Alice"))).isPositive();
        assertThat(personne(1L, "Alice").compareTo(personne(3L, "Alice"))).isZero();
    }

    @Test
    @DisplayName("equality and hashCode are based on the name")
    void equalsAndHashCode_ShouldUseName() {
        Personne a = personne(1L, "Alice");
        Personne b = personne(2L, "Alice");
        Personne c = personne(1L, "Bob");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(new Personne()).isEqualTo(new Personne());
    }

    @Test
    @DisplayName("accessors and toString cover the fields")
    void accessors_ShouldRoundTrip() {
        Personne personne = new Personne();
        Date dateN = Date.from(Instant.parse("1980-01-01T00:00:00Z"));
        Pays pays = new Pays();
        personne.setPrenom("prenom");
        personne.setDateN(dateN);
        personne.setProfilePath("/profile");
        personne.setPays(pays);

        assertThat(personne.getPrenom()).isEqualTo("prenom");
        assertThat(personne.getDateN()).isEqualTo(dateN);
        assertThat(personne.getProfilePath()).isEqualTo("/profile");
        assertThat(personne.getPays()).isSameAs(pays);
        assertThat(personne.toString()).contains("prenom=prenom", "profilePath=/profile");
    }
}
