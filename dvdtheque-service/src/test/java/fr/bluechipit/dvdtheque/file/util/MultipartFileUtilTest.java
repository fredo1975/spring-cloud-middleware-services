package fr.bluechipit.dvdtheque.file.util;

import exceptions.DvdthequeCommonsException;
import fr.bluechipit.dvdtheque.model.ExcelFilmHandler;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultipartFileUtil Unit Tests")
class MultipartFileUtilTest {

    @Mock
    private ExcelFilmHandler excelFilmHandler;

    private MultipartFileUtil buildUtil() {
        MultipartFileUtil util = new MultipartFileUtil();
        ReflectionTestUtils.setField(util, "excelFilmHandler", excelFilmHandler);
        return util;
    }

    @Test
    @DisplayName("createFileToImport() doit retourner directement un fichier csv")
    void createFileToImport_ShouldReturnCsvFile() throws Exception {
        MultipartFileUtil util = buildUtil();
        MockMultipartFile file = new MockMultipartFile("file", "films.csv", "text/csv", "a;b\r\n".getBytes());

        File result = util.createFileToImport(file);

        assertThat(result).exists();
        assertThat(result.getName()).isEqualTo("films.csv");
        assertThat(Files.readString(result.toPath())).isEqualTo("a;b\r\n");
        result.delete();
    }

    @Test
    @DisplayName("createFileToImport() doit convertir un xlsx en csv via ExcelFilmHandler")
    void createFileToImport_ShouldConvertXlsxToCsv() throws Exception {
        MultipartFileUtil util = buildUtil();
        given(excelFilmHandler.createSheetFromFile(any(File.class))).willReturn(new XSSFWorkbook());
        given(excelFilmHandler.createCsvFromExcel(any(Workbook.class))).willReturn("converted;content\r\n");
        MockMultipartFile file = new MockMultipartFile("file", "films.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "binary".getBytes());

        File result = util.createFileToImport(file);

        assertThat(result).exists();
        assertThat(result.getName()).endsWith("_films.xlsx");
        assertThat(Files.readString(result.toPath())).isEqualTo("converted;content\r\n");
        result.delete();
    }

    @Test
    @DisplayName("createFileToImport() doit aussi accepter l'extension xls")
    void createFileToImport_ShouldAcceptXls() throws Exception {
        MultipartFileUtil util = buildUtil();
        given(excelFilmHandler.createSheetFromFile(any(File.class))).willReturn(new XSSFWorkbook());
        given(excelFilmHandler.createCsvFromExcel(any(Workbook.class))).willReturn("xls;content\r\n");
        MockMultipartFile file = new MockMultipartFile("file", "films.xls", "application/vnd.ms-excel", "binary".getBytes());

        File result = util.createFileToImport(file);

        assertThat(result).exists();
        assertThat(result.getName()).endsWith("_films.xls");
        result.delete();
    }

    @Test
    @DisplayName("createFileToImport() doit lever une exception pour une extension inconnue")
    void createFileToImport_ShouldThrowForUnknownExtension() {
        MultipartFileUtil util = buildUtil();
        MockMultipartFile file = new MockMultipartFile("file", "films.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> util.createFileToImport(file))
                .isInstanceOf(DvdthequeCommonsException.class)
                .hasMessageContaining("File not recognized");
    }
}
