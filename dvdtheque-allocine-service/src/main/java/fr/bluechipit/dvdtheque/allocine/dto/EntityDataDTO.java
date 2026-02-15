package fr.bluechipit.dvdtheque.allocine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EntityDataDTO(int id,
                            String year,
                            @JsonProperty("poster_path") String posterPath,
                            @JsonProperty("director_name") List<String> directorName,
                            @JsonProperty("first_release") String firstRelease,
                            String thumbnail,
                            @JsonProperty("is_program") Boolean isProgram,
                            List<String> activities,
                            String nationality) {
}
