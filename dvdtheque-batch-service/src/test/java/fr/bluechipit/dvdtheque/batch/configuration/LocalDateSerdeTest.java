package fr.bluechipit.dvdtheque.batch.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateSerdeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        mapper.registerModule(module);
    }

    @Test
    @DisplayName("LocalDateSerializer writes an ISO local date")
    void serialize_ShouldWriteIsoDate() throws Exception {
        String json = mapper.writeValueAsString(LocalDate.of(2021, 1, 30));

        assertThat(json).isEqualTo("\"2021-01-30\"");
    }

    @Test
    @DisplayName("LocalDateDeserializer reads an ISO local date")
    void deserialize_ShouldReadIsoDate() throws Exception {
        LocalDate date = mapper.readValue("\"2021-01-30\"", LocalDate.class);

        assertThat(date).isEqualTo(LocalDate.of(2021, 1, 30));
    }
}
