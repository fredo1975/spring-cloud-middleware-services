package fr.bluechipit.dvdtheque.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exceptions métier - Unit Tests")
class ExceptionClassesTest {

    @Test
    @DisplayName("FilmNotFoundException doit porter le message")
    void filmNotFound_ShouldCarryMessage() {
        FilmNotFoundException ex = new FilmNotFoundException("Film introuvable");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex).hasMessage("Film introuvable");
    }

    @Test
    @DisplayName("FilmNotFoundException doit porter le message et la cause")
    void filmNotFound_ShouldCarryCause() {
        IllegalStateException cause = new IllegalStateException("cause");
        FilmNotFoundException ex = new FilmNotFoundException("Film introuvable", cause);

        assertThat(ex).hasMessage("Film introuvable").hasCause(cause);
    }

    @Test
    @DisplayName("ResourceNotFoundException doit porter le message")
    void resourceNotFound_ShouldCarryMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Ressource introuvable");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex).hasMessage("Ressource introuvable");
    }
}
