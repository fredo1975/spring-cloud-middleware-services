package fr.bluechipit.dvdtheque.batch.film.backup;

import enums.DvdFormat;
import enums.FilmOrigine;
import model.Dvd;
import model.Film;
import model.Personne;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelFilmHandlerTest {

    private final ExcelFilmHandler handler = new ExcelFilmHandler();

    private Set<Personne> personnes(String... noms) {
        Set<Personne> personnes = new LinkedHashSet<>();
        for (String nom : noms) {
            Personne personne = new Personne();
            personne.setNom(nom);
            personnes.add(personne);
        }
        return personnes;
    }

    private Film film() {
        Film film = new Film();
        film.setTitre("T");
        film.setAnnee(2012);
        film.setTmdbId(1L);
        film.setOrigine(FilmOrigine.DVD);
        film.setVu(true);
        film.setDateVue(LocalDate.of(2013, 8, 1));
        film.setRealisateur(personnes("A"));
        film.setActeur(personnes("B"));
        return film;
    }

    private String[] dataRow(String csv) {
        String[] lines = csv.split("\r\n", -1);
        return lines[1].split(";", -1);
    }

    @Test
    @DisplayName("createByteContentFromFilmList produces a readable workbook and csv")
    void createByteContentFromFilmList_ShouldProduceReadableWorkbook() throws Exception {
        Film film = film();
        Dvd dvd = new Dvd();
        dvd.setZone(2);
        dvd.setFormat(DvdFormat.DVD);
        film.setDvd(dvd);

        byte[] bytes = handler.createByteContentFromFilmList(List.of(film));
        Workbook workbook = handler.createSheetFromByteArray(bytes);
        String csv = handler.createCsvFromExcel(workbook);
        String[] row = dataRow(csv);

        assertThat(csv).startsWith(String.join(";", ExcelFilmHandler.EXCEL_HEADER_TAB));
        assertThat(row).hasSize(14);
        assertThat(row[0]).isEqualTo("A");
        assertThat(row[1]).isEqualTo("T");
        assertThat(row[6]).isEqualTo("oui");
        assertThat(row[7]).isEqualTo("01/08/2013");
        assertThat(row[9]).isEqualTo("2");
        assertThat(row[10]).isEqualTo("non");
        assertThat(row[12]).isEqualTo("DVD");
    }

    @Test
    @DisplayName("writeBook throws when a LocalDate is passed to SimpleDateFormat (current behaviour)")
    void writeBook_WithDateInsertion_ShouldThrowIllegalArgumentException() {
        Film film = film();
        film.setDateInsertion(LocalDate.of(2013, 10, 1));
        handler.initSheet(handler.getWorkBook());

        assertThatThrownBy(() -> handler.writeBook(film))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("writeBook throws when the rip date is a LocalDate (current behaviour)")
    void writeBook_WithRipDate_ShouldThrowIllegalArgumentException() {
        Film film = film();
        Dvd dvd = new Dvd();
        dvd.setRipped(true);
        dvd.setDateRip(LocalDate.of(2013, 8, 1));
        film.setDvd(dvd);
        handler.initSheet(handler.getWorkBook());

        assertThatThrownBy(() -> handler.writeBook(film))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("writeBook throws when the DVD release date is a LocalDate (current behaviour)")
    void writeBook_WithDateSortieDvd_ShouldThrowIllegalArgumentException() {
        Film film = film();
        Dvd dvd = new Dvd();
        dvd.setZone(2);
        film.setDvd(dvd);
        film.setDateSortieDvd(LocalDate.of(2013, 8, 1));
        handler.initSheet(handler.getWorkBook());

        assertThatThrownBy(() -> handler.writeBook(film))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getWorkBook returns a non null workbook")
    void getWorkBook_ShouldReturnWorkbook() {
        assertThat(handler.getWorkBook()).isNotNull();
    }
}
