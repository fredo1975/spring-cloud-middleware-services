package specifications.filter;

import exceptions.NotImplementedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchCriteria Unit Tests")
class SearchCriteriaTest {

    @Test
    @DisplayName("Le constructeur doit résoudre l'opération et exposer les champs")
    void constructor_ShouldResolveOperationAndExposeFields() {
        SearchCriteria criteria = new SearchCriteria("titre", "eq", "INCEPTION", "AND");

        assertThat(criteria.getKey()).isEqualTo("titre");
        assertThat(criteria.getOperation()).isEqualTo(FilterOperation.EQUAL);
        assertThat(criteria.getValue()).isEqualTo("INCEPTION");
        assertThat(criteria.getPredicate()).isEqualTo("AND");
    }

    @Test
    @DisplayName("Le constructeur doit lever NotImplementedException pour une opération invalide")
    void constructor_ShouldThrowForInvalidOperation() {
        assertThatThrownBy(() -> new SearchCriteria("titre", "bad", "v", "AND"))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    @DisplayName("isOrPredicate() doit être insensible à la casse")
    void isOrPredicate_ShouldBeCaseInsensitive() {
        assertThat(new SearchCriteria("k", "eq", "v", "OR").isOrPredicate()).isTrue();
        assertThat(new SearchCriteria("k", "eq", "v", "or").isOrPredicate()).isTrue();
        assertThat(new SearchCriteria("k", "eq", "v", "AND").isOrPredicate()).isFalse();
    }

    @Test
    @DisplayName("isOrPredicate() quirk: lève une NPE quand le prédicat est nul")
    void isOrPredicate_ShouldThrowNullPointerForNullPredicate() {
        SearchCriteria criteria = new SearchCriteria("k", "eq", "v", null);

        assertThatThrownBy(criteria::isOrPredicate).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Les setters doivent mettre à jour les champs")
    void setters_ShouldUpdateFields() {
        SearchCriteria criteria = new SearchCriteria("k", "eq", "v", "AND");

        criteria.setKey("annee");
        criteria.setOperation(FilterOperation.IN);
        criteria.setValue("2020");
        criteria.setPredicate("OR");

        assertThat(criteria.getKey()).isEqualTo("annee");
        assertThat(criteria.getOperation()).isEqualTo(FilterOperation.IN);
        assertThat(criteria.getValue()).isEqualTo("2020");
        assertThat(criteria.getPredicate()).isEqualTo("OR");
    }
}
