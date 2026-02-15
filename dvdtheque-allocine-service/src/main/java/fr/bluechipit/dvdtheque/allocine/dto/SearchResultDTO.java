package fr.bluechipit.dvdtheque.allocine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResultDTO(String entityType,
                              String entityId,
                              String gid,
                              String label,
                              String facet,
                              String originalLabel,
                              List<String> textSearchData,
                              int status,
                              long viewcount,
                              int irankpopular,
                              boolean browsable,
                              String lastRelease,
                              EntityDataDTO data,
                              EntityScoresDTO scores,
                              List<String> genres,
                              List<String> tags,
                              String lastUpdatedAt,
                              String id,
                              double score,
                              boolean sponsored,
                              String campaignName,
                              String gender) {
}
