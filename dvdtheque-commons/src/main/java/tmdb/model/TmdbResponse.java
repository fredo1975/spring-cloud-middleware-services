package tmdb.model;

import java.util.List;

public record TmdbResponse(int page, List<Results> results, int total_results, int total_pages) {
}
