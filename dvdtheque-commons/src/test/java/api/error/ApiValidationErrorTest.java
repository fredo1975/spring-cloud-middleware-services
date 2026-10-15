package api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiValidationError Unit Tests")
class ApiValidationErrorTest {

    @Test
    @DisplayName("Le constructeur (objet, message) doit être un ApiSubError")
    void constructor_ShouldSetObjectAndMessage() {
        ApiValidationError error = new ApiValidationError("film", "titre invalide");

        assertThat(error).isInstanceOf(ApiSubError.class);
        assertThat(error.getObject()).isEqualTo("film");
        assertThat(error.getMessage()).isEqualTo("titre invalide");
        assertThat(error.getField()).isNull();
    }

    @Test
    @DisplayName("Les setters doivent mettre à jour les champs")
    void setters_ShouldUpdateFields() {
        ApiValidationError error = new ApiValidationError("film", "msg");

        error.setObject("personne");
        error.setField("nom");
        error.setRejectedValue("x");
        error.setMessage("invalide");

        assertThat(error.getObject()).isEqualTo("personne");
        assertThat(error.getField()).isEqualTo("nom");
        assertThat(error.getRejectedValue()).isEqualTo("x");
        assertThat(error.getMessage()).isEqualTo("invalide");
    }
}
