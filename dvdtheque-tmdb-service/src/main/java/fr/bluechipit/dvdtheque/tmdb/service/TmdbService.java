package fr.bluechipit.dvdtheque.tmdb.service;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tmdb.model.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.bluechipit.dvdtheque.tmdb.controller.TmdbServiceController.*;

@Service
public class TmdbService {
    private static final Logger logger = LoggerFactory.getLogger(TmdbService.class);

    @Autowired
    private Environment environment;

    @Autowired
    private RestTemplate restTemplate;

    public List<Results> searchAllMoviesByTitle(String title) {
        List<Results> allResults = new ArrayList<>();
        int currentPage = 1;
        int totalPages;

        try {
            do {
                SearchResults response = fetchPage(title, currentPage);
                if (response != null && response.getResults() != null) {
                    allResults.addAll(response.getResults());
                    totalPages = response.getTotal_pages();
                } else {
                    break;
                }
                currentPage++;
            } while (currentPage <= totalPages && currentPage <= 10); // Safety cap at 10 pages

            return allResults.stream()
                    .filter(f -> f != null && StringUtils.hasText(f.release_date()))
                    .sorted(Comparator.comparing(Results::release_date).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error during batch search for title: {}", title, e);
            throw new RuntimeException("TMDB Search failed", e);
        }
    }

    private SearchResults fetchPage(String title, int page) {
        String url = UriComponentsBuilder.fromHttpUrl(environment.getRequiredProperty("themoviedb.search.movie.query"))
                .queryParam("api_key", environment.getRequiredProperty("themoviedb.api.key"))
                .queryParam("query", title)
                .queryParam("language", "fr")
                .queryParam("page", page)
                .toUriString();
        return restTemplate.getForObject(url, SearchResults.class);
    }
    @Cacheable(value = "tmdbReleaseDates", key = "#tmdbId")
    public LocalDate fetchBestReleaseDate(Long tmdbId) {
        String url = UriComponentsBuilder.fromHttpUrl(environment.getRequiredProperty(TMDB_MOVIE_QUERY))
                .path("/{tmdbId}/release_dates")
                .queryParam("api_key", environment.getRequiredProperty(TMDB_API_KEY))
                .buildAndExpand(tmdbId)
                .toUriString();

        try {
            ReleaseDates relDates = restTemplate.getForObject(url, ReleaseDates.class);

            if (relDates == null || CollectionUtils.isEmpty(relDates.getResults())) {
                return LocalDate.now(); // Or a specific default date
            }

            List<ReleaseDatesResults> results = relDates.getResults();

            // Priority Logic: FR -> US -> First Available
            ReleaseDatesResults bestCountryMatch = results.stream()
                    .filter(r -> "FR".equalsIgnoreCase(r.getIso_3166_1()))
                    .findFirst()
                    .orElseGet(() -> results.stream()
                            .filter(r -> "US".equalsIgnoreCase(r.getIso_3166_1()))
                            .findFirst()
                            .orElse(results.get(0)));

            if (CollectionUtils.isNotEmpty(bestCountryMatch.getRelease_dates())) {
                String rawDate = bestCountryMatch.getRelease_dates().get(0).getRelease_date();
                // OffsetDateTime handles the ISO 8601 format (2024-05-01T00:00:00.000Z)
                return OffsetDateTime.parse(rawDate).toLocalDate();
            }

        } catch (Exception e) {
            logger.error("Failed to fetch release date for tmdbId: {}", tmdbId, e);
        }

        return LocalDate.now();
    }

    @Cacheable(value = "tmdbCredits", key = "#tmdbId")
    public Credits fetchTmdbCredits(Long tmdbId) {
        logger.info("Fetching credits from TMDB for ID: {}", tmdbId);

        String url = UriComponentsBuilder.fromHttpUrl(environment.getRequiredProperty(TMDB_MOVIE_QUERY))
                .path("/{tmdbId}/credits")
                .queryParam("api_key", environment.getRequiredProperty(TMDB_API_KEY))
                .buildAndExpand(tmdbId)
                .toUriString();

        try {
            return restTemplate.getForObject(url, Credits.class);
        } catch (RestClientException e) {
            logger.error("Error calling TMDB for credits (ID: {}): {}", tmdbId, e.getMessage());
            throw e; // Let the controller handle the status code
        }
    }

    @Cacheable(value = "tmdbSearchResults", key = "{#title, #page}")
    public SearchResults fetchSearchResultsByTitle(String title, Integer page) {
        int pageNumber = (page == null) ? 1 : page;

        String url = UriComponentsBuilder.fromHttpUrl(environment.getRequiredProperty(TMDB_SEARCH_MOVIE_QUERY))
                .queryParam("api_key", environment.getRequiredProperty(TMDB_API_KEY))
                .queryParam("query", title)
                .queryParam("language", "fr")
                .queryParam("page", pageNumber)
                .toUriString();

        try {
            return restTemplate.getForObject(url, SearchResults.class);
        } catch (RestClientException e) {
            logger.error("Error searching TMDB for title '{}' on page {}: {}", title, pageNumber, e.getMessage());
            throw e;
        }
    }

    public boolean remoteResourceExists(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            // execute a HEAD request to check existence without downloading bytes
            restTemplate.headForHeaders(url);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            // Specifically caught if 404
            return false;
        } catch (Exception e) {
            logger.warn("Resource check failed for URL: {}. Reason: {}", url, e.getMessage());
            return false;
        }
    }

    @Cacheable(value = "tmdbMovieDetails", key = "#tmdbId")
    public Optional<ResultsByTmdbId> fetchTmdbMovieById(final Long tmdbId) {
        String url = UriComponentsBuilder.fromHttpUrl(environment.getRequiredProperty(TMDB_MOVIE_QUERY))
                .path("/{tmdbId}")
                .queryParam("api_key", environment.getRequiredProperty(TMDB_API_KEY))
                .queryParam("language", "fr")
                .buildAndExpand(tmdbId)
                .toUriString();

        try {
            ResultsByTmdbId results = restTemplate.getForObject(url, ResultsByTmdbId.class);
            return Optional.ofNullable(results);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Movie with tmdbId={} not found in TMDB", tmdbId);
            return Optional.empty();
        } catch (RestClientException e) {
            logger.error("Error connecting to TMDB for id {}: {}", tmdbId, e.getMessage());
            throw e; // Caught by our GlobalExceptionHandler
        }
    }

}
