package fr.bluechipit.dvdtheque.batch.film.backup;

import exceptions.DvdthequeCommonsException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipartFileUtilTest {

    private MultipartFileUtil util;
    private final List<File> filesToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        util = new MultipartFileUtil();
        ReflectionTestUtils.setField(util, "excelFilmHandler", new ExcelFilmHandler());
    }

    @AfterEach
    void tearDown() {
        filesToDelete.forEach(file -> {
            if (file.exists()) {
                file.delete();
            }
        });
    }

    private byte[] xlsx(String... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Films");
            int rowIndex = 0;
            for (String row : rows) {
                Row sheetRow = sheet.createRow(rowIndex++);
                String[] cells = row.split(";", -1);
                for (int i = 0; i < cells.length; i++) {
                    sheetRow.createCell(i).setCellValue(cells[i]);
                }
            }
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    @DisplayName("a csv upload is returned as is")
    void createFileToImport_Csv_ShouldReturnUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ListeDvd.csv", "text/csv",
                "a;b;c".getBytes(StandardCharsets.UTF_8));

        File result = util.createFileToImport(file);
        filesToDelete.add(result);

        assertThat(result).exists();
        assertThat(result.getName()).endsWith("ListeDvd.csv");
        assertThat(Files.readString(result.toPath())).isEqualTo("a;b;c");
    }

    @Test
    @DisplayName("an xlsx upload is converted to a csv temp file")
    void createFileToImport_Xlsx_ShouldConvertToCsv() throws Exception {
        byte[] content = xlsx("A;B;C", "X;Y;Z");
        MockMultipartFile file = new MockMultipartFile("file", "backup.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        File result = util.createFileToImport(file);
        filesToDelete.add(result);

        assertThat(result).exists();
        assertThat(result.getName()).endsWith("_backup.xlsx");
        assertThat(Files.readString(result.toPath())).isEqualTo("A;B;C\r\nX;Y;Z\r\n");
    }

    @Test
    @DisplayName("an unsupported extension throws a DvdthequeCommonsException")
    void createFileToImport_UnsupportedExtension_ShouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> util.createFileToImport(file))
                .isInstanceOf(DvdthequeCommonsException.class)
                .hasMessageContaining("File not recognized");
    }

    @Test
    @DisplayName("a file that cannot be materialised on disk throws IllegalStateException")
    void createFileToImport_MissingFile_ShouldThrow() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("ghost-" + UUID.randomUUID() + ".csv");
        doNothing().when(file).transferTo(any(File.class));

        assertThatThrownBy(() -> util.createFileToImport(file))
                .isInstanceOf(IllegalStateException.class);
    }
}
