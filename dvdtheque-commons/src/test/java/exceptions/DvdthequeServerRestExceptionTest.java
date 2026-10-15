package exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("DvdthequeServerRestException Unit Tests")
class DvdthequeServerRestExceptionTest {

    @Test
    @DisplayName("quirk: le constructeur lève toujours IllegalArgumentException(\"Invalid entries\")")
    void constructor_ShouldAlwaysThrowInvalidEntries() {
        assertThatThrownBy(() -> new DvdthequeServerRestException(String.class, "id", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid entries");
    }

    @Test
    @DisplayName("quirk: la levée est inconditionnelle, même sans paramètre")
    void constructor_ShouldThrowEvenWithoutParameters() {
        Throwable thrown = catchThrowable(() -> new DvdthequeServerRestException(String.class));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid entries");
    }
}
