package fr.bluechipit.dvdtheque.dao.specifications.filter;

import fr.bluechipit.dvdtheque.dao.specifications.exceptions.ParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpecificationsBuilder Unit Tests")
class SpecificationsBuilderTest {

    private SpecificationsBuilder<Object> builder;

    @BeforeEach
    void setUp() {
        SpecificationFactory<Object> factory = new SpecificationFactory<>();
        ReflectionTestUtils.invokeMethod(factory, "init");
        builder = new SpecificationsBuilder<>();
        ReflectionTestUtils.setField(builder, "specificationFactory", factory);
    }

    @Test
    @DisplayName("with() doit être fluent et construire une spécification pour un seul critère")
    void with_ShouldBeFluentAndBuildSingleCriteria() {
        Specification<Object> spec = builder.with("titre:eq:INCEPTION:AND,").build();

        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("build() doit chaîner plusieurs critères AND/OR")
    void build_ShouldChainMultipleCriteria() {
        Specification<Object> spec = builder
                .with("titre:eq:A:AND,annee:eq:2020:OR,")
                .build();

        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("build() doit lever ParameterException quand la requête ne matche aucun critère")
    void build_ShouldThrowParameterExceptionWhenNoCriteria() {
        SpecificationsBuilder<Object> b = builder.with("garbage");

        assertThatThrownBy(b::build).isInstanceOf(ParameterException.class);
    }

    @Test
    @DisplayName("build() quirk: NPE quand with() n'a jamais été appelé")
    void build_ShouldThrowNullPointerWhenWithNeverCalled() {
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("with(Map) quirk: la concaténation clé+valeur ne produit aucun critère")
    void withMap_ShouldNotProduceCriteria() {
        Map<String, Object> params = new HashMap<>();
        params.put("titre", "INCEPTION");

        SpecificationsBuilder<Object> b = builder.with(params);

        assertThatThrownBy(b::build).isInstanceOf(ParameterException.class);
    }
}
