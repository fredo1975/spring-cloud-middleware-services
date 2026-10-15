package api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiError Unit Tests")
class ApiErrorTest {

    @Test
    @DisplayName("Le constructeur avec statut doit positionner le statut et l'horodatage")
    void statusConstructor_ShouldSetStatusAndTimestamp() {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST);

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getMessage()).isNull();
    }

    @Test
    @DisplayName("Le constructeur (statut, exception) doit utiliser un message générique")
    void statusAndThrowableConstructor_ShouldUseGenericMessage() {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND, new RuntimeException("boom"));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getMessage()).isEqualTo("Unexpected error");
        assertThat(error.getDebugMessage()).isEqualTo("boom");
    }

    @Test
    @DisplayName("Le constructeur (statut, message, exception) doit conserver le message fourni")
    void statusMessageAndThrowableConstructor_ShouldKeepMessage() {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "custom", new RuntimeException("boom"));

        assertThat(error.getMessage()).isEqualTo("custom");
        assertThat(error.getDebugMessage()).isEqualTo("boom");
    }

    @Test
    @DisplayName("Les setters doivent mettre à jour les champs")
    void setters_ShouldUpdateFields() {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST);

        error.setStatus(HttpStatus.CONFLICT);
        error.setMessage("msg");
        error.setDebugMessage("debug");
        error.setSubErrors(java.util.List.of());

        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.getMessage()).isEqualTo("msg");
        assertThat(error.getDebugMessage()).isEqualTo("debug");
        assertThat(error.getSubErrors()).isEmpty();
    }
}
