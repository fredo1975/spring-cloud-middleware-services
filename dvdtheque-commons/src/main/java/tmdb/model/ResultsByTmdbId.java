package tmdb.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ResultsByTmdbId(Long id, String title, String original_title, String poster_path, String release_date, String overview, int runtime, @JsonProperty("genres") List<Genres> genres, String homepage) {
}
