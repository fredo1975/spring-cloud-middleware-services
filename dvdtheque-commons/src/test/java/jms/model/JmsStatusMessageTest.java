package jms.model;

import enums.JmsStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JmsStatusMessage Unit Tests")
class JmsStatusMessageTest {

    @Test
    @DisplayName("Le constructeur doit exposer tous les champs")
    void constructor_ShouldExposeFields() {
        JmsStatusMessage<String> message = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film", 10L, 0);

        assertThat(message.getStatus()).isEqualTo(JmsStatus.IMPORT_INIT);
        assertThat(message.getFilm()).isEqualTo("film");
        assertThat(message.getTiming()).isEqualTo(10L);
        assertThat(message.getStatusValue()).isZero();
    }

    @Test
    @DisplayName("equals/hashCode reposent uniquement sur film et status")
    void equalsAndHashCode_ShouldUseFilmAndStatusOnly() {
        JmsStatusMessage<String> a = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film", 10L, 0);
        JmsStatusMessage<String> b = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film", 99L, 1);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("Un status différent rend les messages non égaux")
    void differentStatus_ShouldNotBeEqual() {
        JmsStatusMessage<String> a = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film", 0L, 0);
        JmsStatusMessage<String> b = new JmsStatusMessage<>(JmsStatus.IMPORT_COMPLETED_SUCCESS, "film", 0L, 0);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Un film différent rend les messages non égaux")
    void differentFilm_ShouldNotBeEqual() {
        JmsStatusMessage<String> a = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film1", 0L, 0);
        JmsStatusMessage<String> b = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film2", 0L, 0);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Deux messages avec film null et status identiques sont égaux")
    void nullFilmsWithSameStatus_ShouldBeEqual() {
        JmsStatusMessage<String> a = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, null, 0L, 0);
        JmsStatusMessage<String> b = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, null, 0L, 0);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("toString() doit contenir le statut")
    void toString_ShouldContainStatus() {
        JmsStatusMessage<String> message = new JmsStatusMessage<>(JmsStatus.IMPORT_INIT, "film", 0L, 0);

        assertThat(message.toString()).contains("IMPORT_INIT");
    }
}
