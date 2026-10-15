package dto;

import enums.FilmFilterCriteriaType;
import enums.FilmOrigine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FilmFilterCriteriaDto Unit Tests")
class FilmFilterCriteriaDtoTest {

    @Test
    @DisplayName("Tous les filtres renseignés doivent produire les 7 types de critères")
    void allFiltersShouldProduceAllCriteriaTypes() {
        FilmFilterCriteriaDto dto = new FilmFilterCriteriaDto("Dune", 2021, 1L, 2L,
                Boolean.TRUE, Boolean.FALSE, FilmOrigine.DVD);

        assertThat(dto.getFilmFilterCriteriaTypeSet()).containsExactlyInAnyOrder(
                FilmFilterCriteriaType.TITRE,
                FilmFilterCriteriaType.REALISATEUR,
                FilmFilterCriteriaType.ANNEE,
                FilmFilterCriteriaType.ACTEUR,
                FilmFilterCriteriaType.RIPPED,
                FilmFilterCriteriaType.RIPPED_SINCE,
                FilmFilterCriteriaType.ORIGINE);
    }

    @Test
    @DisplayName("Seul un titre non vide doit produire le critère TITRE")
    void onlyNonEmptyTitreShouldBeAdded() {
        FilmFilterCriteriaDto dto = new FilmFilterCriteriaDto("Dune", null, null, null,
                null, null, null);

        assertThat(dto.getFilmFilterCriteriaTypeSet()).containsExactly(FilmFilterCriteriaType.TITRE);
    }

    @Test
    @DisplayName("Un titre vide ne doit produire aucun critère")
    void emptyTitreShouldNotBeAdded() {
        FilmFilterCriteriaDto dto = new FilmFilterCriteriaDto("", null, null, null,
                null, null, null);

        assertThat(dto.getFilmFilterCriteriaTypeSet()).isNull();
    }

    @Test
    @DisplayName("quirk: sans aucun filtre, l'ensemble des critères reste null")
    void noFilterShouldLeaveCriteriaSetNull() {
        FilmFilterCriteriaDto dto = new FilmFilterCriteriaDto(null, null, null, null,
                null, null, null);

        assertThat(dto.getFilmFilterCriteriaTypeSet()).isNull();
    }

    @Test
    @DisplayName("Les accesseurs doivent retourner les valeurs fournies")
    void gettersShouldReturnProvidedValues() {
        FilmFilterCriteriaDto dto = new FilmFilterCriteriaDto("Dune", 2021, 1L, 2L,
                Boolean.TRUE, Boolean.FALSE, FilmOrigine.DVD);

        assertThat(dto.getTitre()).isEqualTo("Dune");
        assertThat(dto.getAnnee()).isEqualTo(2021);
        assertThat(dto.getSelectedRealisateur()).isEqualTo(1L);
        assertThat(dto.getSelectedActeur()).isEqualTo(2L);
        assertThat(dto.getSelectedRipped()).isTrue();
        assertThat(dto.getSelectedRippedSince()).isFalse();
        assertThat(dto.getSelectedFilmOrigine()).isEqualTo(FilmOrigine.DVD);
    }

    @Test
    @DisplayName("addFilmFilterCriteriaType() doit initialiser l'ensemble de façon paresseuse")
    void addFilmFilterCriteriaType_ShouldLazilyInitializeSet() {
        FilmFilterCriteriaDto dto = new FilmFilterCriteriaDto(null, null, null, null,
                null, null, null);

        dto.addFilmFilterCriteriaType(FilmFilterCriteriaType.TITRE);

        assertThat(dto.getFilmFilterCriteriaTypeSet()).containsExactly(FilmFilterCriteriaType.TITRE);
    }
}
