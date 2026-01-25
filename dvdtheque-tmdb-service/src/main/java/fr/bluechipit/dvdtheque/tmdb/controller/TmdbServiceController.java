package fr.bluechipit.dvdtheque.tmdb.controller;

import fr.bluechipit.dvdtheque.tmdb.service.TmdbService;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import tmdb.model.Credits;
import tmdb.model.Results;
import tmdb.model.ResultsByTmdbId;
import tmdb.model.SearchResults;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dvdtheque-tmdb-service")
public class TmdbServiceController {
	protected Logger logger = LoggerFactory.getLogger(TmdbServiceController.class);
	public static String TMDB_SEARCH_MOVIE_QUERY="themoviedb.search.movie.query";
	public static String TMDB_API_KEY="themoviedb.api.key";
	public static String TMDB_MOVIE_QUERY="themoviedb.movie.query";

	private final TmdbService tmdbService;

	public TmdbServiceController(Environment environment, RestTemplate restTemplate, TmdbService tmdbService){
		this.tmdbService=tmdbService;
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/retrieveTmdbFilm/byTmdbId")
	public ResponseEntity<ResultsByTmdbId> retrieveTmdbFilm(@RequestParam(name="tmdbId") Long tmdbId) {
		return tmdbService.fetchTmdbMovieById(tmdbId)
				.map(ResponseEntity::ok)
				.orElseGet(() -> {
					logger.error("Film with tmdbId={} not found", tmdbId);
					return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				});
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/retrieveTmdbFrReleaseDate/byTmdbId")
	public ResponseEntity<LocalDate> retrieveTmdbFrReleaseDate(@RequestParam(name="tmdbId") Long tmdbId) {
		// The controller no longer needs to know HOW the date is found
		LocalDate releaseDate = tmdbService.fetchBestReleaseDate(tmdbId);
		return ResponseEntity.ok(releaseDate);
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/retrieveTmdbCredits/byTmdbId")
	public ResponseEntity<Credits> retrieveTmdbCredits(@RequestParam(name="tmdbId") Long tmdbId) {
		return ResponseEntity.ok(tmdbService.fetchTmdbCredits(tmdbId));
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/retrieveTmdbFilmListByTitle/byTitle")
	public ResponseEntity<List<Results>> retrieveTmdbFilmListByTitle(@RequestParam String title) {
		return ResponseEntity.ok(tmdbService.searchAllMoviesByTitle(title));
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/retrieveTmdbSearchResultsByTitle/byTitle")
	public ResponseEntity<SearchResults> retrieveTmdbSearchResultsByTitle(
			@RequestParam(name="title") String title,
			@RequestParam(name="page", required = false) Integer page) {

		return ResponseEntity.ok(tmdbService.fetchSearchResultsByTitle(title, page));
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/checkIfPosterExists/byPosterPath")
	public ResponseEntity<Boolean> checkIfPosterExists(@RequestParam String posterPath) {
		return ResponseEntity.ok(tmdbService.remoteResourceExists(posterPath));
	}

	@RolesAllowed({"user", "batch"})
	@GetMapping("/checkIfProfileImageExists/byPosterPath")
	public ResponseEntity<Boolean> checkIfProfileImageExists(@RequestParam String profilePath) {
		return ResponseEntity.ok(tmdbService.remoteResourceExists(profilePath));
	}
}
