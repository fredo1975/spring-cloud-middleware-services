package fr.bluechipit.dvdtheque.batch.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilmCsvImportFormatTest {

    @Test
    @DisplayName("all csv columns round trip through the accessors")
    void accessors_ShouldRoundTrip() {
        FilmCsvImportFormat format = new FilmCsvImportFormat();
        format.setRealisateur("realisateur");
        format.setTitre("titre");
        format.setOrigine("DVD");
        format.setZonedvd(2);
        format.setAnnee(2012);
        format.setActeurs("acteurs");
        format.setRipped("oui");
        format.setRipDate("01/08/2013");
        format.setDateSortieDvd("05/08/2013");
        format.setDateInsertion("01/10/2013");
        format.setFilmFormat("DVD");
        format.setTmdbId(1234L);
        format.setVu("oui");
        format.setDateVue("10/08/2013");

        assertThat(format.getRealisateur()).isEqualTo("realisateur");
        assertThat(format.getTitre()).isEqualTo("titre");
        assertThat(format.getOrigine()).isEqualTo("DVD");
        assertThat(format.getZonedvd()).isEqualTo(2);
        assertThat(format.getAnnee()).isEqualTo(2012);
        assertThat(format.getActeurs()).isEqualTo("acteurs");
        assertThat(format.getRipped()).isEqualTo("oui");
        assertThat(format.getRipDate()).isEqualTo("01/08/2013");
        assertThat(format.getDateSortieDvd()).isEqualTo("05/08/2013");
        assertThat(format.getDateInsertion()).isEqualTo("01/10/2013");
        assertThat(format.getFilmFormat()).isEqualTo("DVD");
        assertThat(format.getTmdbId()).isEqualTo(1234L);
        assertThat(format.getVu()).isEqualTo("oui");
        assertThat(format.getDateVue()).isEqualTo("10/08/2013");
    }

    @Test
    @DisplayName("toString renders every column")
    void toString_ShouldRenderAllColumns() {
        FilmCsvImportFormat format = new FilmCsvImportFormat();
        format.setTitre("titre");

        assertThat(format.toString())
                .contains("titre=titre")
                .contains("realisateur=", "origine=", "zonedvd=", "annee=", "acteurs=", "ripped=",
                        "ripDate=", "dateSortieDvd=", "dateInsertion=", "filmFormat=", "tmdbId=",
                        "vu=", "dateVue=");
    }
}
