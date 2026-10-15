package fr.bluechipit.dvdtheque.batch.writer;

import enums.DvdFormat;
import enums.FilmOrigine;
import model.Dvd;
import model.Film;
import model.Personne;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelStreamFilmWriterTest {

    private final ExcelStreamFilmWriter writer = new ExcelStreamFilmWriter();

    private Set<Personne> personnes(String... noms) {
        Set<Personne> personnes = new LinkedHashSet<>();
        for (String nom : noms) {
            Personne personne = new Personne();
            personne.setNom(nom);
            personnes.add(personne);
        }
        return personnes;
    }

    private String[] dataRow(String csv) {
        String[] lines = csv.split("\r\n", -1);
        assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
        return lines[1].split(";", -1);
    }

    private String[] headerRow(String csv) {
        String[] lines = csv.split("\r\n", -1);
        return lines[0].split(";", -1);
    }

    @Test
    @DisplayName("a full DVD film is exported with all 14 columns")
    void writeBook_FullDvdFilm_ShouldFillEveryColumn() throws Exception {
        Film film = new Film();
        film.setTitre("12 HOMMES EN COLERE");
        film.setAnnee(1957);
        film.setTmdbId(389L);
        film.setOrigine(FilmOrigine.DVD);
        film.setVu(true);
        film.setDateVue(LocalDate.of(2021, 1, 30));
        film.setDateInsertion(LocalDate.of(1957, 6, 15));
        film.setRealisateur(personnes("SIDNEY LUMET"));
        film.setActeur(personnes("HENRY FONDA", "LEE J. COBB"));
        Dvd dvd = new Dvd();
        dvd.setZone(2);
        dvd.setRipped(true);
        dvd.setDateRip(LocalDate.of(2021, 2, 5));
        dvd.setFormat(DvdFormat.DVD);
        film.setDvd(dvd);
        film.setDateSortieDvd(LocalDate.of(2020, 6, 24));

        byte[] bytes = writer.createByteContentFromFilmList(List.of(film));
        Workbook workbook = writer.createSheetFromByteArray(bytes);
        String csv = writer.createCsvFromExcel(workbook);
        String[] header = headerRow(csv);
        String[] row = dataRow(csv);

        assertThat(header).containsExactly(ExcelStreamFilmWriter.EXCEL_HEADER_TAB);
        assertThat(row).hasSize(14);
        assertThat(row[0]).isEqualTo("SIDNEY LUMET");
        assertThat(row[1]).isEqualTo("12 HOMMES EN COLERE");
        assertThat(row[2]).isEqualTo("1957");
        assertThat(row[3]).isEqualTo("HENRY FONDA,LEE J. COBB");
        assertThat(row[4]).isEqualTo("DVD");
        assertThat(row[5]).isEqualTo("389");
        assertThat(row[6]).isEqualTo("oui");
        assertThat(row[7]).isEqualTo("30/01/2021");
        assertThat(row[8]).isEqualTo("15/06/1957");
        assertThat(row[9]).isEqualTo("2");
        assertThat(row[10]).isEqualTo("oui");
        assertThat(row[11]).isEqualTo("05/02/2021");
        assertThat(row[12]).isEqualTo("DVD");
        assertThat(row[13]).isEqualTo("24/06/2020");
    }

    @Test
    @DisplayName("a non DVD film leaves zone/ripped/format blank but still emits rip date and sort date")
    void writeBook_NonDvdOrigin_ShouldLeaveZoneColumnsBlank() throws Exception {
        Film film = new Film();
        film.setTitre("2001");
        film.setAnnee(1968);
        film.setTmdbId(62L);
        film.setOrigine(FilmOrigine.TV);
        film.setVu(false);
        film.setRealisateur(personnes("STANLEY KUBRICK"));
        film.setActeur(personnes("WILLIAM SYLVESTER"));
        Dvd dvd = new Dvd();
        dvd.setZone(2);
        dvd.setRipped(true);
        dvd.setDateRip(LocalDate.of(2021, 2, 5));
        dvd.setFormat(DvdFormat.DVD);
        film.setDvd(dvd);
        film.setDateSortieDvd(LocalDate.of(2020, 6, 24));

        byte[] bytes = writer.createByteContentFromFilmList(List.of(film));
        String csv = writer.createCsvFromExcel(writer.createSheetFromByteArray(bytes));
        String[] row = dataRow(csv);

        assertThat(row).hasSize(14);
        assertThat(row[4]).isEqualTo("TV");
        assertThat(row[6]).isEqualTo("non");
        assertThat(row[7]).isEmpty();
        assertThat(row[8]).isEmpty();
        assertThat(row[9]).isEmpty();
        assertThat(row[10]).isEmpty();
        assertThat(row[11]).isEqualTo("05/02/2021");
        assertThat(row[12]).isEmpty();
        assertThat(row[13]).isEqualTo("24/06/2020");
    }

    @Test
    @DisplayName("empty realisateur/acteur sets render empty cells")
    void writeBook_WithoutPersonnes_ShouldRenderEmptyCells() throws Exception {
        Film film = new Film();
        film.setTitre("FILM");
        film.setAnnee(2020);
        film.setTmdbId(1L);
        film.setOrigine(FilmOrigine.DVD);
        film.setVu(false);
        Dvd dvd = new Dvd();
        dvd.setZone(1);
        dvd.setRipped(false);
        film.setDvd(dvd);

        byte[] bytes = writer.createByteContentFromFilmList(List.of(film));
        String csv = writer.createCsvFromExcel(writer.createSheetFromByteArray(bytes));
        String[] row = dataRow(csv);

        assertThat(row[0]).isEmpty();
        assertThat(row[3]).isEmpty();
        assertThat(row[9]).isEqualTo("1");
        assertThat(row[10]).isEqualTo("non");
        assertThat(row[11]).isEmpty();
        assertThat(row[12]).isEmpty();
        assertThat(row[13]).isEmpty();
    }

    @Test
    @DisplayName("createSXSSFWorkbookFromFilmList returns a workbook (closed after generation)")
    void createSXSSFWorkbookFromFilmList_ShouldReturnWorkbook() throws Exception {
        Film film = new Film();
        film.setTitre("FILM");
        film.setAnnee(2020);
        film.setTmdbId(1L);

        assertThat(writer.createSXSSFWorkbookFromFilmList(List.of(film))).isNotNull();
    }

    @Test
    @DisplayName("createSheetFromFile reads an existing workbook sheet")
    void createSheetFromFile_ShouldReadWorkbook() throws Exception {
        byte[] bytes = writer.createByteContentFromFilmList(List.of());
        java.io.File temp = java.io.File.createTempFile("excel-writer-test", ".xlsx");
        temp.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(temp)) {
            fos.write(bytes);
        }

        Workbook workbook = writer.createSheetFromFile(temp);
        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getSheetName()).isEqualTo("Films");
        assertThat(sheet.getRow(0)).isNotNull();
        workbook.close();
    }
}
