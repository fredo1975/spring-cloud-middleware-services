package fr.bluechipit.dvdtheque.allocine.dto;

import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CritiquePresseRecTest {

    @Test
    @DisplayName("fromEntity returns null for a null entity")
    void fromEntity_NullEntity_ReturnsNull() {
        assertThat(CritiquePresseRec.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("fromEntity maps every field")
    void fromEntity_MapsAllFields() {
        CritiquePresse critique = new CritiquePresse();
        critique.setId(7);
        critique.setNewsSource("Télérama");
        critique.setAuthor("Marie Martin");
        critique.setBody("Un chef-d'oeuvre");
        critique.setRating(5.0);

        CritiquePresseRec rec = CritiquePresseRec.fromEntity(critique);

        assertThat(rec).isNotNull();
        assertThat(rec.id()).isEqualTo(7);
        assertThat(rec.newsSource()).isEqualTo("Télérama");
        assertThat(rec.author()).isEqualTo("Marie Martin");
        assertThat(rec.body()).isEqualTo("Un chef-d'oeuvre");
        assertThat(rec.rating()).isEqualTo(5.0);
    }
}
