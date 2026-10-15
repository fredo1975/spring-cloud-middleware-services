package fr.bluechipit.dvdtheque.dao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("CritiquesPresse (entité domaine) Unit Tests")
class CritiquesPresseTest {

    private CritiquesPresse critique(Double note, String nomSource) {
        CritiquesPresse cp = new CritiquesPresse();
        cp.setNote(note);
        cp.setNomSource(nomSource);
        return cp;
    }

    @Test
    @DisplayName("compareTo() doit trier par note décroissante")
    void compareTo_ShouldSortByNoteDescending() {
        CritiquesPresse high = critique(5.0, "A");
        CritiquesPresse low = critique(3.0, "B");

        assertThat(high.compareTo(low)).isNegative();
        assertThat(low.compareTo(high)).isPositive();
    }

    @Test
    @DisplayName("compareTo() doit départager par nomSource en cas de note égale")
    void compareTo_ShouldBreakTiesByNomSource() {
        CritiquesPresse a = critique(4.0, "ZZZ");
        CritiquesPresse b = critique(4.0, "AAA");

        assertThat(a.compareTo(b)).isPositive();
        assertThat(b.compareTo(a)).isNegative();
    }

    @Test
    @DisplayName("compareTo() quirk: NPE quand la note est nulle")
    void compareTo_ShouldThrowWhenNoteIsNull() {
        CritiquesPresse withoutNote = critique(null, "A");
        CritiquesPresse withNote = critique(3.0, "B");

        assertThatNullPointerException().isThrownBy(() -> withoutNote.compareTo(withNote));
    }

    @Test
    @DisplayName("equals()/hashCode() reposent sur l'id")
    void equalsAndHashCode_ShouldUseId() {
        CritiquesPresse a = new CritiquesPresse();
        a.setId(1L);
        CritiquesPresse b = new CritiquesPresse();
        b.setId(1L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
