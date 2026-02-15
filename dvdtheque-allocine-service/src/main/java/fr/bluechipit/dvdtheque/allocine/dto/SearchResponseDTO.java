package fr.bluechipit.dvdtheque.allocine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponseDTO(boolean error,
                                String message,
                                List<SearchResultDTO> results) {
}
