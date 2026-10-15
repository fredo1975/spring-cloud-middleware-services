package fr.bluechipit.dvdtheque.batch.writer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class StringHeaderWriterTest {

    @Test
    @DisplayName("writeHeader writes the configured header verbatim")
    void writeHeader_ShouldWriteHeader() throws Exception {
        StringWriter writer = new StringWriter();

        new StringHeaderWriter("gender;nom;annee").writeHeader(writer);

        assertThat(writer.toString()).isEqualTo("gender;nom;annee");
    }
}
