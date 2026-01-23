package tmdb.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Results(Long id, String title, String original_title, String poster_path, String release_date, String overview, int runtime,
                      @JsonProperty("genre_ids")List<Long> genres, String homepage) {
}
