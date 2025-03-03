package fr.bluechipit.dvdtheque.controller;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;

import enums.DvdFormat;
import enums.FilmOrigine;
import exceptions.DvdthequeServerRestException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.bluechipit.dvdtheque.allocine.model.CritiquePresseDto;
import fr.bluechipit.dvdtheque.allocine.model.DvdBuilder;
import fr.bluechipit.dvdtheque.allocine.model.FicheFilmDto;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.file.util.MultipartFileUtil;
import fr.bluechipit.dvdtheque.model.ExcelFilmHandler;
import fr.bluechipit.dvdtheque.service.impl.IFilmService;
import fr.bluechipit.dvdtheque.service.impl.IPersonneService;
import fr.bluechipit.dvdtheque.model.CritiquePresse;
import tmdb.model.*;
import utils.DateUtils;

@RestController
//@ComponentScan({ "fr.fredos.dvdtheque" })
@RequestMapping("/dvdtheque-service")
public class FilmController {
	protected Logger logger = LoggerFactory.getLogger(FilmController.class);
	public static String TMDB_SERVICE_URL = "tmdb-service.url";
	public static String TMDB_SERVICE_BY_TITLE = "tmdb-service.byTitle";
	public static String TMDB_SERVICE_BY_TITLE_BY_PAGE = "tmdb-service.byTitle-byPage";
	public static String TMDB_SERVICE_RELEASE_DATE = "tmdb-service.release-date";
	public static String TMDB_SERVICE_CREDITS = "tmdb-service.get-credits";
	public static String TMDB_SERVICE_RESULTS = "tmdb-service.get-results";
	public static String DVDTHEQUE_BATCH_SERVICE_URL = "dvdtheque-batch-service.url";
	public static String DVDTHEQUE_BATCH_SERVICE_IMPORT = "dvdtheque-batch-service.import";
	public static String ALLOCINE_SERVICE_URL = "allocine-service.url";
	public static String ALLOCINE_SERVICE_BY_TITLE = "allocine-service.byTitle";
	public static String ALLOCINE_SERVICE_BY_ID = "allocine-service.byId";
	private static String NB_ACTEURS = "batch.save.nb.acteurs";
	@Autowired
	Environment environment;
	@Autowired
	private IFilmService filmService;
	@Autowired
	protected IPersonneService personneService;
	@Autowired
	private ExcelFilmHandler excelFilmHandler;
	@Autowired
	private MultipartFileUtil multipartFileUtil;
	@Value("${eureka.instance.instance-id}")
	private String instanceId;
	@Value("${limit.film.size}")
	private int limitFilmSize;
	@Autowired
	private RestTemplate restTemplate;
	private Map<Integer, Genres> genresById;

	public Map<Integer, Genres> getGenresById() {
		return genresById;
	}

	public void loadGenres() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("genres.json");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
		List<Genres> l = objectMapper.readValue(bufferedReader, new TypeReference<List<Genres>>() {
		});
		genresById = new HashMap<Integer, Genres>(l.size());
		for (Genres genres : l) {
			genresById.put(genres.getId(), genres);
		}
	}

	public FilmController() throws JsonParseException, JsonMappingException, IOException {
		loadGenres();
	}

	@RolesAllowed("user")
	@GetMapping("/films/byPersonne")
	ResponseEntity<Personne> findPersonne(@RequestParam(name = "nom", required = true) String nom) {
		try {
			return ResponseEntity.ok(personneService.findPersonneByName(nom));
		} catch (Exception e) {
			logger.error(format("an error occured while findPersonne nom='%s' ", nom), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@GetMapping("/films/genres")
	ResponseEntity<List<Genre>> findAllGenres() {
		try {
			return ResponseEntity.ok(filmService.findAllGenres());
		} catch (Exception e) {
			logger.error(format("an error occured while findAllGenres"), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed({ "user", "batch" })
	@PutMapping("/films/cleanAllfilms")
	void cleanAllFilms() {
		filmService.cleanAllFilms();
	}

	@RolesAllowed("user")
	@GetMapping("/films/allocine/byId")
	ResponseEntity<Set<CritiquePresseDto>> findAllCritiquePresseByAllocineFilmById(@RequestParam(name = "id", required = true) Integer id){
		ResponseEntity<FicheFilmDto> ficheFilmDtoResponse = restTemplate.exchange(
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
						+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_ID) + "?id=" + id,
				HttpMethod.GET, null, new ParameterizedTypeReference<FicheFilmDto>() {});
		if(ficheFilmDtoResponse.getBody() != null) {
			return ResponseEntity.ok(ficheFilmDtoResponse.getBody().getCritiquePresse());
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}
	
	@RolesAllowed("user")
	@GetMapping("/films/allocine/byTitle")
	ResponseEntity<List<FicheFilmDto>> findAllCritiquePresseByAllocineFilmByTitle(@RequestParam(name = "title", required = true) String title){
		ResponseEntity<List<FicheFilmDto>> ficheFilmDtoResponse = restTemplate.exchange(
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
						+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE) + "?title=" + title+"&titleO="+title,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<FicheFilmDto>>() {});
		if(ficheFilmDtoResponse.getBody() != null) {
			return ResponseEntity.ok(ficheFilmDtoResponse.getBody());
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}
	
	@RolesAllowed("user")
	@GetMapping("/films/tmdb/byTitre/{titre}")
	ResponseEntity<List<Film>> findTmdbFilmByTitre(@PathVariable String titre){
		List<Film> films = null;
		try {
			ResponseEntity<List<Results>> resultsResponse = restTemplate.exchange(
					environment.getRequiredProperty(TMDB_SERVICE_URL)
							+ environment.getRequiredProperty(TMDB_SERVICE_BY_TITLE) + "?title=" + titre,
					HttpMethod.GET, null, new ParameterizedTypeReference<List<Results>>() {});
			if (resultsResponse != null && CollectionUtils.isNotEmpty(resultsResponse.getBody())) {
				List<Results> results = resultsResponse.getBody();
				films = new ArrayList<>(results.size());
				Set<Long> tmdbIds = results.stream().map(r -> r.getId()).collect(Collectors.toSet());
				Set<Long> tmdbFilmAlreadyInDvdthequeSet = filmService.findAllTmdbFilms(tmdbIds);
				for (Results res : results) {
					Film transformedFilm = transformTmdbFilmToDvdThequeFilm(null, res, tmdbFilmAlreadyInDvdthequeSet,
							false);
					if (transformedFilm != null) {
						films.add(transformedFilm);
					}
				}
			}
			
			return ResponseEntity.ok(films);
		} catch (Exception e) {
			logger.error(format("an error occured while findTmdbFilmByTitre titre='%s' ", titre), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@RolesAllowed("user")
	@GetMapping("/films/tmdb/byTitre/{titre}/{page}")
	ResponseEntity<List<Film>> findTmdbFilmByTitreByPage(@PathVariable String titre,@PathVariable Integer page){
		List<Film> films = null;
		try {
			ResponseEntity<SearchResults> searchResultsResponse = restTemplate.getForEntity(environment.getRequiredProperty(TMDB_SERVICE_URL)
							+ environment.getRequiredProperty(TMDB_SERVICE_BY_TITLE_BY_PAGE) + "?title=" + titre+ "&page="+page, 
							SearchResults.class);
			if (searchResultsResponse != null && searchResultsResponse.getBody()!= null) {
				var searchResults = searchResultsResponse.getBody();
				films = new ArrayList<>(searchResults.getResults().size());
				Set<Long> tmdbIds = searchResults.getResults().stream().map(r -> r.getId()).collect(Collectors.toSet());
				Set<Long> tmdbFilmAlreadyInDvdthequeSet = filmService.findAllTmdbFilms(tmdbIds);
				for (Results res : searchResults.getResults()) {
					Film transformedFilm = transformTmdbFilmToDvdThequeFilm(null, res, tmdbFilmAlreadyInDvdthequeSet,
							false);
					if (transformedFilm != null) {
						films.add(transformedFilm);
					}
				}
			}
			return ResponseEntity.ok(films);
		} catch (Exception e) {
			logger.error(format("an error occured while findTmdbFilmByTitreByPage titre='%s' page='%s'", titre, page), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@GetMapping("/films/byId/{id}")
	ResponseEntity<Film> findFilmById(@PathVariable Long id) {
		try {
			return ResponseEntity.ok(processRetrieveCritiquePresse(id, (film,set) -> addCritiquePresseToFilm(set, film), null));
		} catch (Exception e) {
			logger.error(format("an error occured while findFilmById id='%s' ", id), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	private void addCritiquePresseToFilm(Set<CritiquePresseDto> cpDtoSet,Film film) {
		if(CollectionUtils.isNotEmpty(cpDtoSet)) {
			for(CritiquePresseDto cto : cpDtoSet) {
				CritiquePresse cp = new CritiquePresse();
				cp.setAuthor(cto.getAuthor());
				cp.setBody(cto.getBody());
				cp.setRating(cto.getRating());
				cp.setNewsSource(cto.getNewsSource());
				film.addCritiquePresse(cp);
			}
			Collections.sort(film.getCritiquePresse(),new Comparator<CritiquePresse>(){
				@Override
				public int compare(CritiquePresse o1, CritiquePresse o2) {
					return o1.getRating().compareTo(o2.getRating());
				}
			});
		}
	}
	private Film processRetrieveCritiquePresse(Long id,BiConsumer<Film,Set<CritiquePresseDto>> consumer, Film updatedFilm) {
		Film film = filmService.findFilm(id);
		if(film != null) {
			if(film.getAllocineFicheFilmId() != null) {
				ResponseEntity<FicheFilmDto> ficheFilmDtoResponse = restTemplate.exchange(
						environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
								+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_ID) + "?id=" + film.getAllocineFicheFilmId(),
						HttpMethod.GET, null, new ParameterizedTypeReference<FicheFilmDto>() {});
				if(ficheFilmDtoResponse.getBody() != null) {
					Set<CritiquePresseDto> cpDtoSet = ficheFilmDtoResponse.getBody().getCritiquePresse();
					consumer.accept(film,cpDtoSet);
				}
			}else {
				ResponseEntity<List<FicheFilmDto>> ficheFilmDtoResponse = restTemplate.exchange(
						environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
								+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE) + "?title=" + film.getTitre()+"&titleO="+ film.getTitreO(),
						HttpMethod.GET, null, new ParameterizedTypeReference<List<FicheFilmDto>>() {});
				if(ficheFilmDtoResponse.getBody() != null && CollectionUtils.isNotEmpty(ficheFilmDtoResponse.getBody())) {
					Set<CritiquePresseDto> cpDtoSet = ficheFilmDtoResponse.getBody().get(0).getCritiquePresse();
					consumer.accept(film,cpDtoSet);
				}
			}
		}
		if(updatedFilm == null) {
			return film;
		}
		
		return updatedFilm;
	}

	@RolesAllowed("user")
	@GetMapping("/films/byTmdbId/{tmdbid}")
	ResponseEntity<Boolean> checkIfTmdbFilmExists(@PathVariable Long tmdbid) {
		try {
			return ResponseEntity.ok(filmService.checkIfTmdbFilmExists(tmdbid));
		} catch (Exception e) {
			logger.error(format("an error occured while checkIfTmdbFilmExists tmdbid='%s' ", tmdbid), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@GetMapping("/personnes")
	ResponseEntity<List<Personne>> findAllPersonne() {
		try {
			return ResponseEntity.ok(personneService.findAllPersonne());
		} catch (Exception e) {
			logger.error(format("an error occured while findAllPersonne"), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@PutMapping("/films/tmdb/{tmdbId}")
	ResponseEntity<Film> replaceFilm(@RequestBody Film film, @PathVariable Long tmdbId){
		try {
			Film filmOptional = filmService.findFilm(film.getId());
			if (filmOptional == null) {
				return ResponseEntity.notFound().build();
			}
			Results results = restTemplate.getForObject(
					environment.getRequiredProperty(TMDB_SERVICE_URL)
							+ environment.getRequiredProperty(TMDB_SERVICE_RESULTS) + "?tmdbId=" + tmdbId,
					Results.class);
			Film toUpdateFilm = transformTmdbFilmToDvdThequeFilm(film, results, new HashSet<Long>(), true);
			if (toUpdateFilm != null) {
				toUpdateFilm.setOrigine(film.getOrigine());
				filmService.updateFilm(toUpdateFilm);
				return ResponseEntity.ok(toUpdateFilm);
			}
			return ResponseEntity.ok(toUpdateFilm);
		} catch (Exception e) {
			logger.error("an error occured while replacing film tmdbId=" + tmdbId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed({ "user", "batch" })
	@PutMapping("/transformTmdbFilmToDvdThequeFilm/tmdb/{tmdbId}")
	ResponseEntity<Film> transformTmdbFilmToDvdThequeFilm(@RequestBody Results results, @PathVariable Long tmdbId) {
		Film filmToSave = null;
		try {
			filmToSave = transformTmdbFilmToDvdThequeFilm(null, results, new HashSet<Long>(), true);
			return ResponseEntity.ok(filmToSave);
		} catch (Exception e) {
			logger.error("an error occured while transformTmdbFilmToDvdThequeFilm film tmdbId=" + tmdbId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * create a dvdtheque Film based on a TMBD film
	 * 
	 * @param film
	 * @param results
	 * @param tmdbFilmAlreadyInDvdthequeSet
	 * @param persistPersonne
	 * @return
	 * @throws ParseException
	 */
	public Film transformTmdbFilmToDvdThequeFilm(Film film, final Results results,
			final Set<Long> tmdbFilmAlreadyInDvdthequeSet, final boolean persistPersonne) throws ParseException {
		Film transformedfilm = new Film();
		if (film != null && film.getId() != null) {
			transformedfilm.setId(film.getId());
			transformedfilm.setDateInsertion(film.getDateInsertion());
			transformedfilm.setAllocineFicheFilmId(film.getAllocineFicheFilmId());
			transformedfilm.setDateSortieDvd(film.getDateSortieDvd());
		}
		if (film == null) {
			transformedfilm.setId(results.getId());
		}
		if (CollectionUtils.isNotEmpty(tmdbFilmAlreadyInDvdthequeSet)
				&& tmdbFilmAlreadyInDvdthequeSet.contains(results.getId())) {
			transformedfilm.setAlreadyInDvdtheque(true);
		}
		transformedfilm.setTitre(StringUtils.upperCase(results.getTitle()));
		transformedfilm.setTitreO(StringUtils.upperCase(results.getOriginal_title()));
		if (film != null && film.getDvd() != null) {
			transformedfilm.setDvd(film.getDvd());
		}
		Date releaseDate = null;
		try {
			// releaseDate = retrieveTmdbFrReleaseDate(results.getId());
			releaseDate = restTemplate.getForObject(
					environment.getRequiredProperty(TMDB_SERVICE_URL)
							+ environment.getRequiredProperty(TMDB_SERVICE_RELEASE_DATE) + "?tmdbId=" + results.getId(),
					Date.class);
		} catch (RestClientException e) {
			logger.error(e.getMessage() + " for id=" + results.getId());
			SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.TMDB_DATE_PATTERN, Locale.FRANCE);
			if (StringUtils.isNotEmpty(results.getRelease_date())) {
				releaseDate = sdf.parse(results.getRelease_date());
			} else {
				releaseDate = sdf.parse("2000-01-01");
			}
		}
		transformedfilm.setAnnee(retrieveYearFromReleaseDate(releaseDate));
		transformedfilm.setDateSortie(DateUtils.clearDate(releaseDate));
		transformedfilm.setPosterPath(
				environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL) + results.getPoster_path());
		transformedfilm.setTmdbId(results.getId());
		transformedfilm.setOverview(results.getOverview());
		try {
			retrieveAndSetCredits(persistPersonne, results, transformedfilm);
		} catch (Exception e) {
			logger.error(e.getMessage() + " for id=" + results.getId() + " won't be displayed");
			return null;
		}

		transformedfilm.setRuntime(results.getRuntime());
		List<Genres> genres = results.getGenres();
		if (CollectionUtils.isNotEmpty(genres)) {
			for (Genres g : genres) {
				Genres _g = this.genresById.get(g.getId());
				if (_g != null) {
					Genre genre = filmService.findGenre(_g.getId());
					if (genre == null) {
						genre = filmService.saveGenre(new Genre(_g.getId(), _g.getName()));
					}
					transformedfilm.getGenre().add(genre);
				} else {
					logger.error("genre " + g.getName() + " not found in loaded genres");
				}
			}
		}
		if (StringUtils.isNotEmpty(results.getHomepage())) {
			transformedfilm.setHomepage(results.getHomepage());
		}
		if(film != null && film.getDateVue() != null) {
			transformedfilm.setDateVue(film.getDateVue());
		}
		return transformedfilm;
	}

	private void retrieveAndSetCredits(final boolean persistPersonne, final Results results,
			final Film transformedfilm) {
		Credits credits = restTemplate.getForObject(
				environment.getRequiredProperty(TMDB_SERVICE_URL)
						+ environment.getRequiredProperty(TMDB_SERVICE_CREDITS) + "?tmdbId=" + results.getId(),
				Credits.class);
		if (CollectionUtils.isNotEmpty(credits.getCast())) {
			int i = 1;
			for (Cast cast : credits.getCast()) {
				Personne personne = null;
				if (!persistPersonne) {
					personne = personneService.buildPersonne(StringUtils.upperCase(cast.getName()),
							environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL)
									+ cast.getProfile_path());
					personne.setId(Long.valueOf(cast.getCast_id()));
				} else {
					personne = personneService.createOrRetrievePersonne(StringUtils.upperCase(cast.getName()),
							environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL)
									+ cast.getProfile_path());
				}
				transformedfilm.getActeur().add(personne);
				if (i++ == Integer.parseInt(environment.getRequiredProperty(NB_ACTEURS))) {
					break;
				}
			}
		}
		if (CollectionUtils.isNotEmpty(credits.getCrew())) {
			List<Crew> crew = retrieveTmdbDirectors(credits);
			for (Crew c : crew) {
				Personne realisateur = null;
				if (!persistPersonne) {
					realisateur = personneService.buildPersonne(StringUtils.upperCase(c.getName()), null);
					realisateur.setId(RandomUtils.nextLong());
				} else {
					realisateur = personneService.createOrRetrievePersonne(StringUtils.upperCase(c.getName()), null);
				}
				transformedfilm.getRealisateur().add(realisateur);
			}
		}
	}

	public List<Crew> retrieveTmdbDirectors(final Credits credits) {
		return credits.getCrew().stream().filter(cred -> cred.getJob().equalsIgnoreCase("Director"))
				.collect(Collectors.toList());
	}

	private static int retrieveYearFromReleaseDate(final Date relDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(relDate);
		return cal.get(Calendar.YEAR);
	}
	@RolesAllowed("user")
	@GetMapping("/films/search")
	public ResponseEntity<Set<Film>> search(@RequestParam(name = "query", required = true)String query,
			@RequestParam(name = "offset", required = true)Integer offset,
			@RequestParam(name = "limit", required = true)Integer limit,
			@RequestParam(name = "sort", required = true)String sort){
		return ResponseEntity.ok(new HashSet<>(filmService.search(query, offset, limit, sort)));
	}
	@RolesAllowed("user")
	@GetMapping("/films/paginatedSarch")
	public ResponseEntity<Page<Film>> paginatedSarch(@RequestParam(name = "query", required = false)String query,
			@RequestParam(name = "offset", required = false)Integer offset,
			@RequestParam(name = "limit", required = false)Integer limit,
			@RequestParam(name = "sort", required = false)String sort){
		return ResponseEntity.ok(filmService.paginatedSarch(query, offset, limit, sort));
	}
	
	@RolesAllowed("user")
	@PutMapping("/films/update/{id}")
	ResponseEntity<Film> updateFilm(@RequestBody Film film, @PathVariable Long id) {
		try {
			Film mergedFilm = filmService.updateFilm(film);
			Film filmUpdatedWithCritiquePresse = processRetrieveCritiquePresse(id, (f, set) -> {
				addCritiquePresseToFilm(set, mergedFilm);
			},mergedFilm);
			return ResponseEntity.ok(filmUpdatedWithCritiquePresse);
		} catch (Exception e) {
			logger.error("an error occured while updating film id=" + id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@RolesAllowed("user")
	@PutMapping("/films/remove/{id}")
	ResponseEntity<Film> removeFilm(@PathVariable Long id) {
		try {
			Film filmOptional = filmService.findFilm(id);
			if (filmOptional == null) {
				return ResponseEntity.notFound().build();
			}
			filmService.removeFilm(filmOptional);
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			logger.error("an error occured while removing film id=" + id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@PutMapping("/films/cleanCaches")
	ResponseEntity<Void> cleanCaches() {
		try {
			filmService.cleanAllCaches();
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			logger.error("an error occured while cleaning all caches", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@PutMapping("/films/retrieveImage/{id}")
	ResponseEntity<Film> retrieveFilmImage(@PathVariable Long id) {
		try {
			Film film = filmService.findFilm(id);
			if (film == null) {
				return ResponseEntity.notFound().build();
			}
			Results results = restTemplate.getForObject(
					environment.getRequiredProperty(TMDB_SERVICE_URL) + environment.getRequiredProperty(TMDB_SERVICE_RESULTS) + "?tmdbId=" + film.getTmdbId(), Results.class);
			film.setPosterPath(
					environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL) + results.getPoster_path());
			return ResponseEntity.ok(filmService.updateFilm(film));
		} catch (Exception e) {
			logger.error("an error occured while retrieving image for film id=" + id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@PutMapping("/films/retrieveAllImages")
	ResponseEntity<Void> retrieveAllFilmImages() {
		Results results = null;
		try {
			
			Page<Film> films = filmService.paginatedSarch("", null, null, "");
			for(int i = 0 ; i<films.getContent().size();i++) {
				Film film = films.getContent().get(i);
				Boolean posterExists = restTemplate.getForObject(
						environment.getRequiredProperty(TMDB_SERVICE_URL) + "?posterPath=" + film.getPosterPath(),
						Boolean.class);
				if (!posterExists) {
					results = restTemplate.getForObject(
							environment.getRequiredProperty(TMDB_SERVICE_URL) + "?tmdbId=" + film.getTmdbId(),
							Results.class);
					film.setPosterPath(environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL)
							+ results.getPoster_path());
				}
				filmService.updateFilm(film);
			}
			
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			logger.error("an error occured while retrieving all images", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed({ "batch" })
	@PostMapping("/films/saveProcessedFilm")
	ResponseEntity<Film> saveProcessedFilm(@RequestBody Film film) {
		Film filmToSave = null;
		try {
			Results results = restTemplate.getForObject(environment.getRequiredProperty(TMDB_SERVICE_URL)
					+ environment.getRequiredProperty(FilmController.TMDB_SERVICE_RESULTS) + "?tmdbId="
					+ film.getTmdbId(), Results.class);
			if (results != null) {
				filmToSave = transformTmdbFilmToDvdThequeFilm(film, 
						results, 
						new HashSet<Long>(), 
						true);
				if (filmToSave != null) {
					filmToSave.setId(null);
					filmToSave.setOrigine(film.getOrigine());
					if(film.getDateInsertion() != null) {
						filmToSave.setDateInsertion(film.getDateInsertion());
					}else {
						filmToSave.setDateInsertion(DateUtils.clearDate(new Date()));
					}
					filmToSave.setVu(film.isVu());
					filmToSave.setDateVue(film.getDateVue());
					filmToSave.setDateSortieDvd(film.getDateSortieDvd());
					Long id = filmService.saveNewFilm(filmToSave);
					logger.info(filmToSave.toString());
					filmToSave.setId(id);
					return ResponseEntity.ok(filmToSave);
				}
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			logger.error("an error occured while saving film tmdbId=" + film.getTmdbId(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed({ "user", "batch" })
	@PutMapping("/films/save/{tmdbId}")
	ResponseEntity<Film> saveFilm(@PathVariable Long tmdbId, @RequestBody String origine){
		Film filmToSave = null;
		try {
			FilmOrigine filmOrigine = FilmOrigine.valueOf(origine);
			if (this.filmService.checkIfTmdbFilmExists(tmdbId)) {
				return ResponseEntity.noContent().build();
			}
			Results results = restTemplate.getForObject(environment.getRequiredProperty(TMDB_SERVICE_URL)
					+ environment.getRequiredProperty(FilmController.TMDB_SERVICE_RESULTS) + "?tmdbId=" + tmdbId,
					Results.class);
			if (results != null) {
				filmToSave = transformTmdbFilmToDvdThequeFilm(null, results, new HashSet<Long>(), true);
				if (filmToSave != null) {
					ResponseEntity<List<FicheFilmDto>> ficheFilmDtoResponse = restTemplate.exchange(
							environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
									+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE) + "?title=" + filmToSave.getTitre()+"&titleO="+ filmToSave.getTitreO(),
							HttpMethod.GET, null, new ParameterizedTypeReference<List<FicheFilmDto>>() {});
					if(ficheFilmDtoResponse.getBody() != null && CollectionUtils.isNotEmpty(ficheFilmDtoResponse.getBody())) {
						filmToSave.setAllocineFicheFilmId(Integer.valueOf(ficheFilmDtoResponse.getBody().get(0).getId()));
					}
					filmToSave.setId(null);
					filmToSave.setOrigine(filmOrigine);
					if (FilmOrigine.DVD.equals(filmOrigine)) {
						Dvd dvd = filmService.buildDvd(filmToSave.getAnnee(), 
								Integer.valueOf(2), 
								null, 
								null,
								DvdFormat.DVD);
						//dvd.setRipped(true);
						//dvd.setDateRip(new Date());
						filmToSave.setDvd(dvd);
					}
					filmToSave.setDateInsertion(DateUtils.clearDate(new Date()));
					filmService.saveNewFilm(filmToSave);
				}
			}
			if (filmToSave == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(filmToSave);
		} catch (Exception e) {
			logger.error("an error occured while saving film tmdbId=" + tmdbId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed({ "user", "batch" })
	@PostMapping("/films/buildDvd")
	ResponseEntity<Dvd> buildDvd(@RequestBody DvdBuilder dvdBuilder){
		try {
			Dvd dvd = filmService.buildDvd(dvdBuilder.getFilmToSave().getAnnee(), 
					dvdBuilder.getZonedvd(), 
					null, 
					null,
					StringUtils.isNotEmpty(dvdBuilder.getFilmFormat()) ? DvdFormat.valueOf(dvdBuilder.getFilmFormat()): null);
			return ResponseEntity.ok(dvd);
		} catch (Exception e) {
			logger.error("an error occured while building dvd ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@RolesAllowed("user")
	@PutMapping("/personnes/byId/{id}")
	ResponseEntity<Object> updatePersonne(@RequestBody Personne p, @PathVariable Long id) {
		Personne personne = personneService.findByPersonneId(id);
		if (personne == null) {
			return ResponseEntity.notFound().build();
		}
		if (StringUtils.isNotEmpty(p.getNom())) {
			personne.setNom(StringUtils.upperCase(p.getNom()));
		}
		personneService.updatePersonne(personne);
		logger.info(personne.toString());
		return ResponseEntity.noContent().build();
	}

	@RolesAllowed("user")
	@PostMapping("/films/import")
	ResponseEntity<Void> importFilmList(@RequestParam("file") MultipartFile file) throws IOException {
		/*File resFile = null;
		try {
			resFile = this.multipartFileUtil.createFileToImport(file);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}*/
		byte[] csvBytes = file.getBytes();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.builder("attachment").filename(file.getOriginalFilename()).build());

		HttpEntity<?> request = new HttpEntity<>(csvBytes, headers);
		ResponseEntity<String> resultsResponse = restTemplate.exchange(
				environment.getRequiredProperty(DVDTHEQUE_BATCH_SERVICE_URL)
						+ environment.getRequiredProperty(DVDTHEQUE_BATCH_SERVICE_IMPORT),
				HttpMethod.POST, request, String.class);
		logger.info(resultsResponse.getBody());
		return ResponseEntity.noContent().build();
	}

	private Page<Film>  paginatedSarch(String query,int pageNumber){
		return filmService.paginatedSarch(query, pageNumber, null, "");
	}
	@RolesAllowed({ "user", "batch" })
	@GetMapping("/films")
	ResponseEntity<List<Film>> getAllFilms(){
		List<Film> list = new ArrayList<>();
		Page<Film> films = paginatedSarch("",1);
		list.addAll(films.getContent());
		while(films.hasNext()) {
			Pageable p = films.nextPageable();
			films = paginatedSarch("",p.getPageNumber()+1);
			list.addAll(films.getContent());
		}
		return ResponseEntity.ok(list);
	}
	@RolesAllowed("user")
	@PostMapping("/films/export")
	ResponseEntity<byte[]> exportFilmList(@RequestBody String origine)
			throws DvdthequeServerRestException, IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentLanguage(Locale.FRANCE);
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		LocalDateTime localDate = LocalDateTime.now();
		String fileName = "ListeDVDExport" + "-" + localDate.getSecond() + "-" + origine + ".xlsx";
		try {
			List<Film> list = new ArrayList<>();
			FilmOrigine filmOrigine = FilmOrigine.valueOf(origine);
			//Page<Film> films;
			if(filmOrigine == FilmOrigine.TOUS) {
				Page<Film> films = paginatedSarch("",1);
				list.addAll(films.getContent());
				while(films.hasNext()) {
					Pageable p = films.nextPageable();
					films = paginatedSarch("",p.getPageNumber()+1);
					list.addAll(films.getContent());
				}
			}else {
				Page<Film> films = paginatedSarch("origine:eq:"+filmOrigine+":AND",1);
				list.addAll(films.getContent());
				while(films.hasNext()) {
					Pageable p = films.nextPageable();
					films = paginatedSarch("origine:eq:"+filmOrigine+":AND",p.getPageNumber()+1);
					list.addAll(films.getContent());
				}
			}
			byte[] excelContent = this.excelFilmHandler.createByteContentFromFilmList(list);
			headers.setContentType(
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
			headers.setContentLength(excelContent.length);
			return new ResponseEntity<byte[]>(excelContent, headers, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("an error occured while exporting film list", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@RolesAllowed("user")
	@PostMapping("/films/search/export")
	ResponseEntity<byte[]> exportFilmSearch(@RequestBody String query)
			throws DvdthequeServerRestException, IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentLanguage(Locale.FRANCE);
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		LocalDateTime localDate = LocalDateTime.now();
		String fileName = "ListeDVDSearch" + "-" + localDate.getSecond() + ".xlsx";
		try {
			List<Film> list = filmService.search(query, 1, 100, "");
			if (list == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
			byte[] excelContent = this.excelFilmHandler.createByteContentFromFilmList(list);
			headers.setContentType(
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
			headers.setContentLength(excelContent.length);
			return new ResponseEntity<byte[]>(excelContent, headers, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("an error occured while exporting film search", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
