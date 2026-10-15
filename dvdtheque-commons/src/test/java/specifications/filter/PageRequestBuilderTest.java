package specifications.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageRequestBuilder Unit Tests")
class PageRequestBuilderTest {

    @Test
    @DisplayName("Un champ sans préfixe doit trier en ASC (page 1-based convertie en index)")
    void plainFieldShouldSortAscending() {
        PageRequest pageRequest = PageRequestBuilder.getPageRequest(10, 2, "titre");

        assertThat(pageRequest.getPageNumber()).isEqualTo(1);
        assertThat(pageRequest.getPageSize()).isEqualTo(10);
        Sort.Order order = pageRequest.getSort().getOrderFor("titre");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Les préfixes - et + doivent définir DESC et ASC")
    void prefixesShouldDefineDirection() {
        PageRequest pageRequest = PageRequestBuilder.getPageRequest(10, 1, "-titre,+annee");

        assertThat(pageRequest.getSort().getOrderFor("titre").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageRequest.getSort().getOrderFor("annee").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("quirk: pageNumber est 1-based, l'index interne vaut pageNumber - 1")
    void pageNumberShouldBeShiftedByOne() {
        assertThat(PageRequestBuilder.getPageRequest(10, 3, "titre").getPageNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un tri absent ou vide doit produire un tri non trié")
    void blankSortingShouldBeUnsorted() {
        assertThat(PageRequestBuilder.getPageRequest(5, 1, null).getSort().isUnsorted()).isTrue();
        assertThat(PageRequestBuilder.getPageRequest(5, 1, "").getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("Les valeurs par défaut doivent être page 0 et taille 20")
    void defaultsShouldBeApplied() {
        PageRequest pageRequest = PageRequestBuilder.getPageRequest(null, null, null);

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(20);
    }
}
