package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CritiquesPresseTest {

    private CritiquesPresse critique(Long id, Double note, String source) {
        CritiquesPresse critique = new CritiquesPresse();
        critique.setId(id);
        critique.setNote(note);
        critique.setNomSource(source);
        return critique;
    }

    @Test
    @DisplayName("compareTo orders by descending note then source")
    void compareTo_ShouldOrderByNoteDescThenSource() {
        CritiquesPresse high = critique(1L, 5.0, "a");
        CritiquesPresse low = critique(2L, 3.0, "b");

        assertThat(high.compareTo(low)).isNegative();
        assertThat(low.compareTo(high)).isPositive();

        CritiquesPresse sameNoteA = critique(3L, 4.0, "a");
        CritiquesPresse sameNoteB = critique(4L, 4.0, "b");
        assertThat(sameNoteA.compareTo(sameNoteB)).isNegative();
        assertThat(sameNoteB.compareTo(sameNoteA)).isPositive();
    }

    @Test
    @DisplayName("equality and hashCode are based on the identifier")
    void equalsAndHashCode_ShouldUseId() {
        CritiquesPresse a = critique(1L, 5.0, "a");
        CritiquesPresse b = critique(1L, 1.0, "b");
        CritiquesPresse c = critique(2L, 5.0, "a");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(new CritiquesPresse()).isEqualTo(new CritiquesPresse());
    }

    @Test
    @DisplayName("accessors and toString cover the fields")
    void accessors_ShouldRoundTrip() {
        CritiquesPresse critique = new CritiquesPresse();
        critique.setCode(1);
        critique.setAuteur("auteur");
        critique.setCritique("critique");
        critique.setNote(4.5);

        assertThat(critique.getCode()).isEqualTo(1);
        assertThat(critique.getAuteur()).isEqualTo("auteur");
        assertThat(critique.getCritique()).isEqualTo("critique");
        assertThat(critique.getNote()).isEqualTo(4.5);
        assertThat(critique.toString()).contains("auteur=auteur", "note=4.5");
    }
}
