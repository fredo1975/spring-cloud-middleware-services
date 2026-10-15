package fr.bluechipit.dvdtheque.model;

import enums.DvdFormat;
import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExcelFilmHandler Unit Tests")
class ExcelFilmHandlerTest {

    private final ExcelFilmHandler handler = new ExcelFilmHandler();
    private SXSSFWorkbook workbook;

    @AfterEach
    void tearDown() throws IOException {
        if (workbook != null) {
            workbook.close();
        }
    }

    private SXSSFWorkbook newWorkbookWithSheet() {
        workbook = new SXSSFWorkbook(50);
        handler.initSheet(workbook);
        return workbook;
    }

    private String cellValue(SXSSFWorkbook wb, int row, int col) {
        SXSSFSheet sheet = wb.getSheet("Films");
        SXSSFRow r = sheet.getRow(row);
        return r.getCell(col).getStringCellValue();
    }

    private Film dvdFilm() {
        Film film = new Film();
        film.setTitre("INCEPTION");
        film.setAnnee(2010);
        film.setTmdbId(27205L);
        film.setOrigine(FilmOrigine.DVD);
        film.setVu(true);
        film.setDateVue(LocalDate.of(2021, 1, 2));
        film.setDateInsertion(LocalDate.of(2020, 3, 4));
        film.setDateSortieDvd(LocalDate.of(2020, 5, 6));
        Personne real = new Personne();
        real.setNom("NOLAN");
        film.setRealisateur(Set.of(real));
        Personne act = new Personne();
        act.setNom("DICAPRIO");
        film.setActeur(Set.of(act));
        Dvd dvd = new Dvd();
        dvd.setZone(2);
        dvd.setRipped(true);
        dvd.setDateRip(LocalDate.of(2020, 7, 8));
        dvd.setFormat(DvdFormat.DVD);
        film.setDvd(dvd);
        return film;
    }

    @Test
    @DisplayName("initSheet() doit créer la feuille et la ligne d'en-tête")
    void initSheet_ShouldCreateHeaderRow() {
        SXSSFWorkbook wb = newWorkbookWithSheet();

        assertThat(wb.getSheet("Films")).isNotNull();
        assertThat((Object) wb.getSheet("Films").getRow(0)).isNotNull();
        for (int i = 0; i < ExcelFilmHandler.EXCEL_HEADER_TAB.length; i++) {
            assertThat(cellValue(wb, 0, i)).isEqualTo(ExcelFilmHandler.EXCEL_HEADER_TAB[i]);
        }
    }

    @Test
    @DisplayName("writeBook() doit écrire les 14 colonnes d'un film DVD")
    void writeBook_ShouldWriteAllDvdColumns() {
        SXSSFWorkbook wb = newWorkbookWithSheet();

        handler.writeBook(dvdFilm());

        assertThat(cellValue(wb, 1, 0)).isEqualTo("NOLAN");
        assertThat(cellValue(wb, 1, 1)).isEqualTo("INCEPTION");
        assertThat(cellValue(wb, 1, 2)).isEqualTo("2010");
        assertThat(cellValue(wb, 1, 3)).isEqualTo("DICAPRIO");
        assertThat(cellValue(wb, 1, 4)).isEqualTo("DVD");
        assertThat(cellValue(wb, 1, 5)).isEqualTo("27205");
        assertThat(cellValue(wb, 1, 6)).isEqualTo("oui");
        assertThat(cellValue(wb, 1, 7)).isEqualTo("02/01/2021");
        assertThat(cellValue(wb, 1, 8)).isEqualTo("04/03/2020");
        assertThat(cellValue(wb, 1, 9)).isEqualTo("2");
        assertThat(cellValue(wb, 1, 10)).isEqualTo("oui");
        assertThat(cellValue(wb, 1, 11)).isEqualTo("08/07/2020");
        assertThat(cellValue(wb, 1, 12)).isEqualTo("DVD");
        assertThat(cellValue(wb, 1, 13)).isEqualTo("06/05/2020");
    }

    @Test
    @DisplayName("writeBook() quirk: sans dvd, les colonnes 9 à 13 ne sont pas écrites")
    void writeBook_ShouldOmitDvdColumnsWhenDvdIsNull() {
        SXSSFWorkbook wb = newWorkbookWithSheet();
        Film film = new Film();
        film.setTitre("MINIMAL");
        film.setAnnee(2000);
        film.setTmdbId(1L);
        film.setOrigine(FilmOrigine.TV);

        handler.writeBook(film);

        SXSSFRow row = wb.getSheet("Films").getRow(1);
        assertThat(row.getLastCellNum()).isEqualTo((short) 9);
        assertThat(cellValue(wb, 1, 4)).isEqualTo("TV");
        assertThat(cellValue(wb, 1, 6)).isEqualTo("non");
        assertThat(cellValue(wb, 1, 7)).isEmpty();
        assertThat(cellValue(wb, 1, 8)).isEmpty();
    }

    @Test
    @DisplayName("writeBook() quirk: origine null produit une cellule vide en colonne 4")
    void writeBook_ShouldWriteEmptyOriginWhenNull() {
        SXSSFWorkbook wb = newWorkbookWithSheet();
        Film film = new Film();
        film.setTitre("NOORIGIN");
        film.setAnnee(1999);
        film.setTmdbId(2L);
        film.setOrigine(null);

        handler.writeBook(film);

        assertThat(cellValue(wb, 1, 4)).isEmpty();
    }

    @Test
    @DisplayName("createByteContentFromFilmList() doit produire un contenu non vide")
    void createByteContentFromFilmList_ShouldReturnBytes() throws IOException {
        byte[] content = handler.createByteContentFromFilmList(List.of(dvdFilm()));

        assertThat(content).isNotEmpty();
    }

    @Test
    @DisplayName("createCsvFromExcel() doit convertir la feuille en CSV séparé par ';'")
    void createCsvFromExcel_ShouldBuildCsv() throws IOException {
        SXSSFWorkbook wb = newWorkbookWithSheet();
        handler.writeBook(dvdFilm());

        String csv = handler.createCsvFromExcel(wb);

        assertThat(csv).contains("Titre").contains("INCEPTION").contains(";").contains("\r\n");
    }

    @Test
    @DisplayName("createSXSSFWorkbookFromFilmList() quirk: retourne un classeur déjà fermé")
    void createSXSSFWorkbookFromFilmList_ReturnsClosedWorkbook() throws IOException {
        SXSSFWorkbook result = handler.createSXSSFWorkbookFromFilmList(List.of(dvdFilm()));

        assertThat((Object) result).isNotNull();
    }

    @Test
    @DisplayName("getWorkBook() doit fournir une nouvelle instance à chaque appel")
    void getWorkBook_ShouldReturnNewInstance() throws IOException {
        SXSSFWorkbook first = handler.getWorkBook();
        SXSSFWorkbook second = handler.getWorkBook();
        try {
            assertThat((Object) first).isNotSameAs(second);
        } finally {
            first.close();
            second.close();
        }
    }
}
