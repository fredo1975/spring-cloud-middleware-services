package specifications.filter;

import exceptions.NotImplementedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FilterOperation Unit Tests")
class FilterOperationTest {

    @Test
    @DisplayName("fromValue() doit être insensible à la casse")
    void fromValue_ShouldBeCaseInsensitive() {
        assertThat(FilterOperation.fromValue("eq")).isEqualTo(FilterOperation.EQUAL);
        assertThat(FilterOperation.fromValue("EQ")).isEqualTo(FilterOperation.EQUAL);
        assertThat(FilterOperation.fromValue("Like")).isEqualTo(FilterOperation.CONTAINS);
    }

    @Test
    @DisplayName("toString() doit retourner la valeur technique")
    void toString_ShouldReturnValue() {
        assertThat(FilterOperation.GREATER_THAN.toString()).isEqualTo("gt");
        assertThat(FilterOperation.NOT_EQUAL.toString()).isEqualTo("neg");
    }

    @Test
    @DisplayName("fromValue() doit lever NotImplementedException pour une opération inconnue")
    void fromValue_ShouldThrowForUnknownOperation() {
        assertThatThrownBy(() -> FilterOperation.fromValue("zzz")).isInstanceOf(NotImplementedException.class);
    }
}
