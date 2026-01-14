package fr.bluechipit.dvdtheque.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.DvdFormat;
import enums.FilmOrigine;
import exceptions.DvdthequeServerRestException;
import fr.bluechipit.dvdtheque.allocine.model.CritiquePresseDto;
import fr.bluechipit.dvdtheque.allocine.model.DvdBuilder;
import fr.bluechipit.dvdtheque.allocine.model.FicheFilmDto;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.file.util.MultipartFileUtil;
import fr.bluechipit.dvdtheque.service.impl.FilmSaveService;
import fr.bluechipit.dvdtheque.service.impl.FilmService;
import fr.bluechipit.dvdtheque.service.impl.PersonneService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tmdb.model.*;
import utils.DateUtils;

import javax.annotation.security.RolesAllowed;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

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

	@Autowired
	Environment environment;
	@Autowired
	private FilmService filmService;
	@Autowired
	private FilmSaveService filmSaveService;
	@Autowired
	protected PersonneService personneService;
	@Autowired
	private MultipartFileUtil multipartFileUtil;
	@Value("${eureka.instance.instance-id}")
	private String instanceId;
	@Value("${limit.film.size}")
	private int limitFilmSize;
	@Autowired
	private RestTemplate restTemplate;
	private Map<Long, Genres> genresById;

	public Map<Long, Genres> getGenresById() {
		return genresById;
	}

	public void loadGenres() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("genres.json");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
		List<Genres> l = objectMapper.readValue(bufferedReader, new TypeReference<List<Genres>>() {
		});
		genresById = l.stream().collect(Collectors.toMap(Genres::id, g -> g));
	}

	public FilmController() throws JsonParseException, JsonMappingException, IOException {
		loadGenres();
	}

	@RolesAllowed("user")
	@GetMapping("/films/byPersonne")
	ResponseEntity<Personne> findPersonne(@RequestParam(name = "nom", required = true) String nom) {
		return ResponseEntity.ok(personneService.findPersonneByName(nom));
	}

	@RolesAllowed("user")
	@GetMapping("/films/genres")
	ResponseEntity<List<Genre>> findAllGenres() {
		return ResponseEntity.ok(filmService.findAllGenres());
	}

	@RolesAllowed({ "user", "batch" })
	@PutMapping("/films/cleanAllfilms")
	void cleanAllFilms() {
		filmService.cleanAllFilms();
	}

	@RolesAllowed("user")
	@GetMapping("/films/allocine/byId")
	ResponseEntity<Set<CritiquePresseDto>> findAllCritiquePresseByAllocineFilmById(@RequestParam(name = "id", required = true) Integer id){
		Set<CritiquePresseDto> res = filmService.findAllCritiquePresseByAllocineFilmById(id);
		if(CollectionUtils.isNotEmpty(res)){
			return ResponseEntity.ok(res);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}
	
	@RolesAllowed("user")
	@GetMapping("/films/allocine/byTitle")
	ResponseEntity<List<FicheFilmDto>> findAllCritiquePresseByAllocineFilmByTitle(@RequestParam(name = "title", required = true) String title){
		List<FicheFilmDto> res = filmService.findAllCritiquePresseByAllocineFilmByTitle(title);
		if(CollectionUtils.isNotEmpty(res)){
			return ResponseEntity.ok(res);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}
	
	@RolesAllowed("user")
	@GetMapping("/films/tmdb/byTitre/{titre}")
	ResponseEntity<List<Film>> findTmdbFilmByTitre(@PathVariable String titre) throws ParseException {
		return ResponseEntity.ok(filmService.findTmdbFilmByTitre(titre,null));
	}
	
	@RolesAllowed("user")
	@GetMapping("/films/tmdb/byTitre/{titre}/{page}")
	ResponseEntity<List<Film>> findTmdbFilmByTitreByPage(@PathVariable String titre,@PathVariable Integer page) throws ParseException {
		return ResponseEntity.ok(filmService.findTmdbFilmByTitre(titre,page));
	}

	@RolesAllowed("user")
	@GetMapping("/films/byId/{id}")
	ResponseEntity<Film> findFilmById(@PathVariable Long id) {
		return ResponseEntity.ok(filmService.processRetrieveCritiquePresse(id, (film,set) -> filmService.addCritiquePresseToFilm(set, film), null));
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
	ResponseEntity<Film> replaceFilm(@RequestBody Film film, @PathVariable Long tmdbId) throws ParseException {
		return ResponseEntity.ok(filmService.replaceFilm(film,tmdbId));
	}

	@RolesAllowed({ "user", "batch" })
	@PutMapping("/transformTmdbFilmToDvdThequeFilm/tmdb/{tmdbId}")
	ResponseEntity<Film> transformTmdbFilmToDvdThequeFilm(@RequestBody Results results, @PathVariable Long tmdbId) throws ParseException {
		return ResponseEntity.ok(filmService.transformTmdbFilmToDvdThequeFilm(null, results, new HashSet<Long>(), true));
	}

	public List<Crew> retrieveTmdbDirectors(final Credits credits) {
		return credits.getCrew().stream().filter(cred -> cred.getJob().equalsIgnoreCase("Director"))
				.collect(Collectors.toList());
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
		return ResponseEntity.ok(filmService.updateFilm(film, id));
	}
	
	@RolesAllowed("user")
	@PutMapping("/films/remove/{id}")
	ResponseEntity<Film> removeFilm(@PathVariable Long id) {
		try {
			Film filmOptional = filmSaveService.findFilm(id);
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
			Film film = filmSaveService.findFilm(id);
			if (film == null) {
				return ResponseEntity.notFound().build();
			}
			Results results = restTemplate.getForObject(
					environment.getRequiredProperty(TMDB_SERVICE_URL) + environment.getRequiredProperty(TMDB_SERVICE_RESULTS) + "?tmdbId=" + film.getTmdbId(), Results.class);
			film.setPosterPath(
					environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL) + results.getPoster_path());
			return ResponseEntity.ok(filmSaveService.updateFilm(film));
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
				filmSaveService.updateFilm(film);
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
				filmToSave = filmService.transformTmdbFilmToDvdThequeFilm(film,
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
					Long id = filmSaveService.saveNewFilm(filmToSave);
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
				filmToSave = filmService.transformTmdbFilmToDvdThequeFilm(null, results, new HashSet<Long>(), true);
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
					filmSaveService.saveNewFilm(filmToSave);
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
	ResponseEntity<Dvd> buildDvd(@RequestBody DvdBuilder dvdBuilder) {
		Dvd dvd = filmService.buildDvd(dvdBuilder.getFilmToSave().getAnnee(),
				dvdBuilder.getZonedvd(),
				null,
				null,
				StringUtils.isNotEmpty(dvdBuilder.getFilmFormat()) ? DvdFormat.valueOf(dvdBuilder.getFilmFormat()): null);
		return ResponseEntity.ok(dvd);
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

	@RolesAllowed({ "user", "batch" })
	@GetMapping("/films")
	ResponseEntity<List<Film>> getAllFilms(){
        Page<Film> films = filmService.paginatedSarch("", 1, null, "");
        List<Film> list = new ArrayList<>(films.getContent());
		while(films.hasNext()) {
			Pageable p = films.nextPageable();
			films = filmService.paginatedSarch("", p.getPageNumber()+1, null, "");
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
		byte[] excelContent = filmService.exportFilmList(origine);
		headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
		headers.setContentLength(excelContent.length);
		return new ResponseEntity<byte[]>(excelContent, headers, HttpStatus.OK);
	}
	
	@RolesAllowed("user")
	@PostMapping("/films/search/export")
	ResponseEntity<byte[]> exportFilmSearch(@RequestBody String query) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentLanguage(Locale.FRANCE);
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		LocalDateTime localDate = LocalDateTime.now();
		String fileName = "ListeDVDSearch" + "-" + localDate.getSecond() + ".xlsx";
		List<Film> list = filmService.search(query, 1, 100, "");
		if (list == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		byte[] excelContent = filmService.exportFilmSearch(query);
		headers.setContentType(
				MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
		headers.setContentLength(excelContent.length);
		return new ResponseEntity<byte[]>(excelContent, headers, HttpStatus.OK);
	}
}
