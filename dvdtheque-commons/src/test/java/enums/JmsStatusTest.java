package enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JmsStatus Unit Tests")
class JmsStatusTest {

    @Test
    @DisplayName("statusValue() doit retourner les valeurs 0/1 attendues")
    void statusValue_ShouldReturnExpectedValues() {
        assertThat(JmsStatus.CLEAN_DB_INIT.statusValue()).isZero();
        assertThat(JmsStatus.CLEAN_DB_COMPLETED.statusValue()).isEqualTo(1);
        assertThat(JmsStatus.IMPORT_COMPLETED_ERROR.statusValue()).isEqualTo(1);
        assertThat(JmsStatus.DB_FILM_WRITER_INIT.statusValue()).isZero();
    }
}
