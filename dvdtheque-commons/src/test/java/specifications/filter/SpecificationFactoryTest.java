package specifications.filter;

import enums.FilmOrigine;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("SpecificationFactory (commons) Unit Tests")
@SuppressWarnings({"unchecked", "rawtypes"})
class SpecificationFactoryTest {

    private SpecificationFactory<Object> factory;

    @BeforeEach
    void setUp() {
        factory = new SpecificationFactory<>();
        ReflectionTestUtils.invokeMethod(factory, "init");
    }

    private Root<Object> rootMock() {
        return mock(Root.class);
    }

    @Test
    @DisplayName("getByCriteria() doit supporter EQUAL, GREATER_THAN, LESS_THAN et IN")
    void getByCriteria_ShouldSupportRegisteredOperations() {
        assertThat(factory.getByCriteria(new SearchCriteria("titre", "eq", "A", "AND"))).isNotNull();
        assertThat(factory.getByCriteria(new SearchCriteria("annee", "gt", "1", "AND"))).isNotNull();
        assertThat(factory.getByCriteria(new SearchCriteria("annee", "lt", "1", "AND"))).isNotNull();
        assertThat(factory.getByCriteria(new SearchCriteria("annee", "in", "1", "AND"))).isNotNull();
    }

    @Test
    @DisplayName("getByCriteria() quirk: NPE pour une opération non enregistrée (neg)")
    void getByCriteria_ShouldThrowNpeForUnsupportedOperation() {
        SearchCriteria criteria = new SearchCriteria("titre", "neg", "A", "AND");

        assertThatThrownBy(() -> factory.getByCriteria(criteria)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("EQUAL sur champ texte doit produire un like en conservant la casse (branche par défaut)")
    void equalOnStringFieldShouldProduceLikePredicate() {
        Root<Object> root = rootMock();
        Path path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        given(root.get("titre")).willReturn(path);
        given(path.getJavaType()).willReturn((Class) String.class);
        given(cb.like(any(), eq("%inception%"))).willReturn(predicate);

        Specification<Object> spec = factory.getByCriteria(new SearchCriteria("titre", "eq", "inception", "AND"));

        assertThat(spec.toPredicate(root, null, cb)).isSameAs(predicate);
        verify(cb).like((Expression<String>) path, "%inception%");
    }

    @Test
    @DisplayName("EQUAL sur champ enum doit convertir la valeur d'origine")
    void equalOnEnumFieldShouldConvertValue() {
        Root<Object> root = rootMock();
        Path path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        given(root.get("origine")).willReturn(path);
        given(path.getJavaType()).willReturn((Class) FilmOrigine.class);
        given(cb.equal(any(Expression.class), any(Object.class))).willReturn(predicate);

        Specification<Object> spec = factory.getByCriteria(new SearchCriteria("origine", "eq", "DVD", "AND"));

        assertThat(spec.toPredicate(root, null, cb)).isSameAs(predicate);
        verify(cb).equal(path, FilmOrigine.DVD);
    }

    @Test
    @DisplayName("EQUAL sur champ Integer doit parser la valeur")
    void equalOnIntegerFieldShouldParseValue() {
        Root<Object> root = rootMock();
        Path path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        given(root.get("annee")).willReturn(path);
        given(path.getJavaType()).willReturn((Class) Integer.class);
        given(cb.equal(any(Expression.class), any(Object.class))).willReturn(predicate);

        Specification<Object> spec = factory.getByCriteria(new SearchCriteria("annee", "eq", "2020", "AND"));

        assertThat(spec.toPredicate(root, null, cb)).isSameAs(predicate);
        verify(cb).equal(path, Integer.valueOf(2020));
    }

    @Test
    @DisplayName("EQUAL sur champ boolean doit parser la valeur")
    void equalOnBooleanFieldShouldParseValue() {
        Root<Object> root = rootMock();
        Path path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        given(root.get("vu")).willReturn(path);
        given(path.getJavaType()).willReturn((Class) boolean.class);
        given(cb.equal(any(Expression.class), any(Object.class))).willReturn(predicate);

        Specification<Object> spec = factory.getByCriteria(new SearchCriteria("vu", "eq", "true", "AND"));

        assertThat(spec.toPredicate(root, null, cb)).isSameAs(predicate);
        verify(cb).equal(path, Boolean.TRUE);
    }

    @Test
    @DisplayName("IN doit construire une clause in().value()")
    void inShouldBuildInClause() {
        Root<Object> root = rootMock();
        Path path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaBuilder.In in = mock(CriteriaBuilder.In.class);
        given(root.get("annee")).willReturn(path);
        given(cb.in(path)).willReturn(in);
        given(in.value("2020")).willReturn(in);

        Specification<Object> spec = factory.getByCriteria(new SearchCriteria("annee", "in", "2020", "AND"));

        assertThat(spec.toPredicate(root, null, cb)).isSameAs(in);
        verify(in).value("2020");
    }
}
