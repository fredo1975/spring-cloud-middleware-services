package fr.bluechipit.dvdtheque.allocine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EntityScoresDTO(@JsonProperty("rating_score") double ratingScore,
                              @JsonProperty("release_score") double releaseScore,
                              @JsonProperty("weekly_rank_score") double weeklyRankScore,
                              @JsonProperty("all_time_rank_score") double allTimeRankScore) {
}
