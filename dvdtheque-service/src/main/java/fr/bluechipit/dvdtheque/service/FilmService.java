package fr.bluechipit.dvdtheque.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import enums.DvdFormat;
import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.allocine.model.CritiquePresseDto;
import fr.bluechipit.dvdtheque.allocine.model.FicheFilmDto;
import fr.bluechipit.dvdtheque.controller.FilmController;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.dao.repository.FilmDao;
import fr.bluechipit.dvdtheque.dao.repository.GenreDao;
import fr.bluechipit.dvdtheque.dao.specifications.filter.SpecificationsBuilder;
import fr.bluechipit.dvdtheque.exception.FilmNotFoundException;
import fr.bluechipit.dvdtheque.model.CritiquePresse;
import fr.bluechipit.dvdtheque.model.ExcelFilmHandler;
import fr.bluechipit.dvdtheque.model.FilmDto;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import specifications.filter.PageRequestBuilder;
import tmdb.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static fr.bluechipit.dvdtheque.controller.FilmController.*;

@Service("filmService")
@CacheConfig(cacheNames = "films")
public class FilmService {
	protected Logger logger = LoggerFactory.getLogger(FilmService.class);

	private static final String NB_ACTEURS = "batch.save.nb.acteurs";
	public static final String CACHE_GENRE = "genreCache";
	IMap<Long, Genre> mapGenres;
	
	private final FilmDao filmDao;
	private final GenreDao genreDao;
	private final PersonneService personneService;
	private final HazelcastInstance instance;
	private final ExcelFilmHandler excelFilmHandler;
	private final RestTemplate restTemplate;
	private final Environment environment;
	private final FilmSaveService filmSaveService;
	private final SpecificationsBuilder<Film> builder;

	public FilmService(FilmDao filmDao, GenreDao genreDao, PersonneService personneService, HazelcastInstance instance, ExcelFilmHandler excelFilmHandler,
					   RestTemplate restTemplate,
					   Environment environment,
					   FilmSaveService filmSaveService,
					   SpecificationsBuilder<Film> builder) {
		this.filmDao = filmDao;
		this.genreDao = genreDao;
		this.personneService = personneService;
		this.instance = instance;
		this.excelFilmHandler = excelFilmHandler;
		this.restTemplate = restTemplate;
		this.environment = environment;
		this.filmSaveService = filmSaveService;
		this.builder = builder;
		this.init();
	}

	public void init() {
		mapGenres = instance.getMap(CACHE_GENRE);
		List<Genre> allGenres = genreDao.findAll();
		for(Genre genre : allGenres) {
			mapGenres.putIfAbsent((long) genre.getTmdbId(), genre);
		}
		logger.info("Loaded {} genres into cache", mapGenres.size());
	}

	public List<FilmDto> getAllFilmDtos() {
		try {
			return filmDao.findAll().stream()
					.map(FilmDto::of)
					.sorted(Comparator.comparing(FilmDto::titre))
					.toList();
		} catch (Exception e) {
			logger.error("Error retrieving films: {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	public List<FilmDto> getAllFilms(){
		Page<FilmDto> films = paginatedDtoSearch("", 1, null, "");
		List<FilmDto> list = new ArrayList<>(films.getContent());
		while(films.hasNext()) {
			Pageable p = films.nextPageable();
			films = paginatedDtoSearch("", p.getPageNumber()+1, null, "");
			list.addAll(films.getContent());
		}
		return list;
	}

	@Transactional(readOnly = true, noRollbackFor = { org.springframework.dao.EmptyResultDataAccessException.class })
	public Film findFilmByTitreWithoutSpecialsCharacters(final String titre) {
		return filmDao.findFilmByTitreWithoutSpecialsCharacters(titre);
	}

	private void mapIdentity(Film existingFilm, Results results, Film transformed, Set<Long> tmdbAlreadyInSet){
		if (existingFilm != null && existingFilm.getId() != null) {
			transformed.setId(existingFilm.getId());
			transformed.setDateInsertion(existingFilm.getDateInsertion());
			transformed.setAllocineFicheFilmId(existingFilm.getAllocineFicheFilmId());
			transformed.setDateSortieDvd(existingFilm.getDateSortieDvd());
		}
		if (existingFilm == null) {
			transformed.setId(results.id());
		}
		if (CollectionUtils.isNotEmpty(tmdbAlreadyInSet)
				&& tmdbAlreadyInSet.contains(results.id())) {
			transformed.setAlreadyInDvdtheque(true);
		}
	}
	/**
	 * create a dvdtheque Film based on a TMBD film
	 *
	 */
	public Film transformTmdbFilmToDvdThequeFilm(Film film, final Results results,
												 final Set<Long> tmdbFilmAlreadyInDvdthequeSet, final boolean persistPersonne) {
		logger.debug("transformTmdbFilmToDvdThequeFilm: {}", film);
		Film transformedfilm = new Film();
		// 1. Map basic identity and existing film data
		mapIdentity(film, results, transformedfilm, tmdbFilmAlreadyInDvdthequeSet);

		// 2. Map Title and Metadata
		transformedfilm.setTitre(StringUtils.upperCase(results.title()));
		transformedfilm.setTitreO(StringUtils.upperCase(results.original_title()));
		transformedfilm.setPosterPath(
				environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL) + results.poster_path());
		transformedfilm.setTmdbId(results.id());
		transformedfilm.setOverview(results.overview());
		transformedfilm.setRuntime(results.runtime());
		transformedfilm.setHomepage(results.homepage());

		if (film != null && film.getDvd() != null) {
			transformedfilm.setDvd(film.getDvd());
		}
		LocalDate releaseDate = fetchBestAvailableDate(results);

		// 3. Handle Dates (Refactored to separate method)
		transformedfilm.setAnnee(releaseDate.getYear());
		transformedfilm.setDateSortie(releaseDate);
		// 4. Map Relationships (Genres and Credits)
		mapGenresToFilm(results, transformedfilm);
		try {
			retrieveAndSetCredits(persistPersonne, results, transformedfilm);
		} catch (Exception e) {
			logger.error(e.getMessage() + " for id=" + results.id() + " won't be displayed");
			return null;
		}

		if(film != null && film.getDateVue() != null) {
			transformedfilm.setDateVue(film.getDateVue());
		}
		return transformedfilm;
	}
	private void mapGenresToFilm(Results results, Film transformed) {
		if (CollectionUtils.isNotEmpty(results.genres())) {
			for (Long id : results.genres()) {
				Genre localGenre = this.mapGenres.get(id);
				if (localGenre != null) {
					transformed.getGenre().add(localGenre);
				} else {
					logger.warn("Genre TMDB ID {} absent de la map locale", id);
				}
			}
		}
	}

	private LocalDate fetchBestAvailableDate(Results results) {
		LocalDate releaseDate = null;
		try {
			// Attempt 1: Specific TMDB Release Date service (FR release usually)
			String url = environment.getRequiredProperty(TMDB_SERVICE_URL)
					+ environment.getRequiredProperty(TMDB_SERVICE_RELEASE_DATE)
					+ "?tmdbId=" + results.id();

			releaseDate = restTemplate.getForObject(url, LocalDate.class);
		} catch (Exception e) {
			logger.warn("Could not fetch specific release date for tmdbId={}, falling back to results object.", results.id());
		}

		// Attempt 2: Use the date string already present in the Results object
		if (releaseDate == null && StringUtils.isNotEmpty(results.release_date())) {
            releaseDate = LocalDate.parse(results.release_date());
        }

		// Attempt 3: Absolute fallback to avoid NullPointerExceptions in your app logic
		if (releaseDate == null) {
			releaseDate = LocalDate.parse("2000-01-01");
		}
		return releaseDate;
	}

	public List<Crew> retrieveTmdbDirectors(final Credits credits) {
		return credits.getCrew().stream().filter(cred -> cred.getJob().equalsIgnoreCase("Director"))
				.collect(Collectors.toList());
	}

	private void retrieveAndSetCredits(final boolean persistPersonne, final Results results,
									   final Film transformedfilm) {
		Credits credits = restTemplate.getForObject(
				environment.getRequiredProperty(TMDB_SERVICE_URL)
						+ environment.getRequiredProperty(TMDB_SERVICE_CREDITS) + "?tmdbId=" + results.id(),
				Credits.class);
        assert credits != null;
        if (CollectionUtils.isNotEmpty(credits.getCast())) {
			int i = 1;
			for (Cast cast : credits.getCast()) {
				Personne personne;
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
				Personne realisateur;
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

	public static Results transformTmdbFilmToResults(final ResultsByTmdbId resultsByTmdbId) {
		if (resultsByTmdbId == null) {
			return null;
		}
		return new Results(resultsByTmdbId.id(),
				resultsByTmdbId.title(),
				resultsByTmdbId.original_title(),
				resultsByTmdbId.poster_path(),
				resultsByTmdbId.release_date(),
				resultsByTmdbId.overview(),
				resultsByTmdbId.runtime(),
				resultsByTmdbId.genres().stream().map(Genres::id).collect(Collectors.toList()),
				resultsByTmdbId.homepage());
	}
	public Optional<FilmDto> saveFilm(Long tmdbId, String origine) {
        if (checkIfTmdbFilmExists(tmdbId)) {
            return Optional.empty();
        }
		FilmOrigine filmOrigine = FilmOrigine.valueOf(origine);
		String tmdbUrl = String.format("%s%s?tmdbId=%s",
				environment.getRequiredProperty(TMDB_SERVICE_URL),
				environment.getRequiredProperty(FilmController.TMDB_SERVICE_RESULTS),
				tmdbId);

		/*
		CompletableFuture<Film> tmdbFuture = CompletableFuture.supplyAsync(() -> {
			ResultsByTmdbId results = restTemplate.getForObject(tmdbUrl, ResultsByTmdbId.class);
			Results res = transformTmdbFilmToResults(results);
			return transformTmdbFilmToDvdThequeFilm(null, res, new HashSet<>(), true);
		});

		CompletableFuture<Integer> ratingFuture = CompletableFuture.supplyAsync(() -> {
			return retrieveAllocineFilmId(filmToSave);
		});*/

		return Optional.ofNullable(restTemplate.getForObject(tmdbUrl, ResultsByTmdbId.class))
				.map(results -> {
					Results res = transformTmdbFilmToResults(results);
					return transformTmdbFilmToDvdThequeFilm(null, res, new HashSet<>(), true);
				})
				.map(filmToSave -> {
					// 3. Enrich with Allocine Data
					enrichWithAllocineId(filmToSave);

					// 4. Apply Business Rules
					prepareFilmForPersistence(filmToSave, filmOrigine);

					// 5. Save
					return FilmDto.of(filmSaveService.saveNewFilm(filmToSave));
				});
    }
/*
	private int retrieveAllocineFilmId(String titre,String titreO) {
		String allocineUrl = String.format("%s%s?title=%s&titleO=%s",
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL),
				environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE),
				titre, titreO);

		ResponseEntity<List<FicheFilmDto>> response = restTemplate.exchange(
				allocineUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<FicheFilmDto>>() {});

		return response.getBody() != null && !response.getBody().isEmpty() ? 0 : response.getBody().getFirst().getId();
	}*/

	private void enrichWithAllocineId(Film film) {
		String allocineUrl = String.format("%s%s?title=%s&titleO=%s",
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL),
				environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE),
				film.getTitre(), film.getTitreO());

		ResponseEntity<List<FicheFilmDto>> response = restTemplate.exchange(
				allocineUrl, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });

		if (response.getBody() != null && !response.getBody().isEmpty()) {
			film.setAllocineFicheFilmId(response.getBody().getFirst().getId());
		}
	}

	private void prepareFilmForPersistence(Film film, FilmOrigine origin) {
		film.setId(null);
		film.setOrigine(origin);
		film.setDateInsertion(LocalDate.now());

		if (FilmOrigine.DVD.equals(origin)) {
			film.setDvd(buildDvd(film.getAnnee(), 2, null, null, DvdFormat.DVD));
		}
	}
	@Transactional()
	public Genre saveGenre(final Genre genre) {
		Genre persistedGenre = genreDao.save(genre);
		mapGenres.putIfAbsent(persistedGenre.getId(), persistedGenre);
		return persistedGenre;
	}
	

	@Transactional(readOnly = true)
	public List<Genre> findAllGenres() {
		Collection<Genre> genres = mapGenres.values();
		logger.debug("genres cache size: " + genres.size());
		if (!genres.isEmpty()) {
			List<Genre> l = new ArrayList<>(genres);
			l.sort(Comparator.comparing(Genre::getName));
			return l;
		}
		logger.debug("no genres find");
		List<Genre> e = this.genreDao.findAll();
		logger.debug("genres size: " + e.size());
		if(CollectionUtils.isNotEmpty(e)) {
			e.forEach(it -> mapGenres.putIfAbsent(it.getId(), it));
		}
		return e;
	}


	@Transactional()
	public void cleanAllFilms() {
		filmDao.deleteAll();
		personneService.cleanAllPersonnes();
	}


	public byte[] exportFilmList(String origine) throws IOException {
		List<Film> list = new ArrayList<>();
		FilmOrigine filmOrigine = FilmOrigine.valueOf(origine);
		//Page<Film> films;
		if(filmOrigine == FilmOrigine.TOUS) {
			Page<Film> films = paginatedSearch("",1, null, "");
			list.addAll(films.getContent());
			while(films.hasNext()) {
				Pageable p = films.nextPageable();
				films = paginatedSearch("",p.getPageNumber()+1, null, "");
				list.addAll(films.getContent());
			}
		}else {
			Page<Film> films = paginatedSearch("origine:eq:"+filmOrigine+":AND",1, null, "");
			list.addAll(films.getContent());
			while(films.hasNext()) {
				Pageable p = films.nextPageable();
				films = paginatedSearch("origine:eq:"+filmOrigine+":AND",p.getPageNumber()+1, null, "");
				list.addAll(films.getContent());
			}
		}
		return this.excelFilmHandler.createByteContentFromFilmList(list);
	}
	public byte[] exportFilmSearch(String query) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentLanguage(Locale.FRANCE);
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		LocalDateTime localDate = LocalDateTime.now();
        List<Film> list = search(query, 1, 100, "");
        if (list == null) {
            return null;
        }
        return this.excelFilmHandler.createByteContentFromFilmList(list);
    }
	@Transactional(readOnly = true)
	public List<Film> search(String query,Integer offset,Integer limit,String sort){
		int limitToSet;
		int offsetToSet;
		String sortToSet;
		if(limit == null) {
			limitToSet = 50;
		}else {
			limitToSet = limit;
		}
		if(offset == null) {
			offsetToSet = 1;
		}else {
			offsetToSet = offset;
		}
		if(StringUtils.isEmpty(sort)) {
			sortToSet = "-dateInsertion";
		}else {
			sortToSet = sort;
		}
		return filmDao.findAll(builder.with(query).build(), 
				PageRequestBuilder.getPageRequest(limitToSet,offsetToSet, sortToSet)).getContent();
	}
	private PageRequest buildDefaultPageRequest(Integer offset,
			Integer limit,
			String sort) {
		int limitToSet;
		int offsetToSet;
		String sortToSet;
		if(limit == null) {
			limitToSet = 50;
		}else {
			limitToSet = limit;
		}
		if(offset == null) {
			offsetToSet = 1;
		}else {
			offsetToSet = offset;
		}
		if(StringUtils.isEmpty(sort)) {
			sortToSet = "-dateInsertion";
		}else {
			sortToSet = sort;
		}
		return PageRequestBuilder.getPageRequest(limitToSet,offsetToSet, sortToSet);
	}

	public Page<FilmDto> paginatedDtoSearch(String query,
			Integer offset,
			Integer limit,
			String sort){
		var page = buildDefaultPageRequest(offset, limit, sort);
		logger.info(page.toString());
		if(StringUtils.isEmpty(query)) {
			return filmDao.findAll(page).map(FilmDto::of);
		}
        return filmDao.findAll(builder.with(query).build(), page).map(FilmDto::of);
	}

	public Page<Film> paginatedSearch(String query,
											Integer offset,
											Integer limit,
											String sort){
		var page = buildDefaultPageRequest(offset, limit, sort);
		if(StringUtils.isEmpty(query)) {
			return filmDao.findAll(page);
		}
		return filmDao.findAll(builder.with(query).build(), page);
	}


	@Transactional()
	public void removeFilm(Long id) {
		Film filmOptional = filmSaveService.findFilm(id);
		filmDao.delete(filmOptional);
	}

	public Set<Long> findAllTmdbFilms(final Set<Long> tmdbIds) {
		return filmDao.findAllTmdbFilms(tmdbIds);
	}


	public Date clearDate(final Date dateToClear) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateToClear);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR, 0);
		return cal.getTime();
	}

	public Dvd buildDvd(final Integer annee, final Integer zone, final String edition, final LocalDate ripDate,
			final DvdFormat dvdFormat) {
		Dvd dvd = new Dvd();
		if (annee != null) {
			dvd.setAnnee(annee);
		}
		if (zone != null) {
			dvd.setZone(zone);
		} else {
			dvd.setZone(21);
		}
		if (StringUtils.isEmpty(edition)) {
			dvd.setEdition("edition");
		} else {
			dvd.setEdition(edition);
		}
		if (ripDate != null) {
			dvd.setDateRip(ripDate);
		}
		if (dvdFormat != null) {
			dvd.setFormat(dvdFormat);
		}
		return dvd;
	}

	public void cleanAllCaches() {
		mapGenres.clear();
	}

	public Boolean checkIfTmdbFilmExists(final Long tmdbId) {
		return filmDao.checkIfTmdbFilmExists(tmdbId).equals(1)?Boolean.TRUE:Boolean.FALSE;
	}

	public Set<CritiquePresseDto> findAllCritiquePresseByAllocineFilmById(Integer id){
		ResponseEntity<FicheFilmDto> ficheFilmDtoResponse = restTemplate.exchange(
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
						+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_ID) + "?id=" + id,
				HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
		if(ficheFilmDtoResponse.getBody() != null) {
			return ficheFilmDtoResponse.getBody().getCritiquePresse();
		}
		return Collections.emptySet();
	}

	public List<FicheFilmDto> findAllCritiquePresseByAllocineFilmByTitle(String title){
		ResponseEntity<List<FicheFilmDto>> ficheFilmDtoResponse = restTemplate.exchange(
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
						+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE) + "?title=" + title+"&titleO="+title,
				HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
		if(ficheFilmDtoResponse.getBody() != null) {
			return ficheFilmDtoResponse.getBody();
		}
		return Collections.emptyList();
	}

	public List<Film> findTmdbFilmByTitre(String titre,Integer page) {
		// 1. Build the URL dynamically
		String baseUrl = environment.getRequiredProperty(TMDB_SERVICE_URL);
		String endpoint = (page == null)
				? environment.getRequiredProperty(TMDB_SERVICE_BY_TITLE) + "?title=" + titre
				: environment.getRequiredProperty(TMDB_SERVICE_BY_TITLE_BY_PAGE) + "?title=" + titre + "&page=" + page;

		// 2. Execute the request
		ResponseEntity<TmdbResponse> response = restTemplate.exchange(
				baseUrl + endpoint,
				HttpMethod.GET,
				null, // This is where the Token Interceptor would help!
                new ParameterizedTypeReference<>() {
                }
		);

		TmdbResponse body = response.getBody();
		if (body == null || CollectionUtils.isEmpty(body.results())) {
			return Collections.emptyList();
		}

		// 3. Batch check existing films to minimize DB hits
		Set<Long> tmdbIds = body.results().stream()
				.map(Results::id)
				.collect(Collectors.toSet());
		Set<Long> alreadyInDvdtheque = findAllTmdbFilms(tmdbIds);

		// 4. Transform and filter
		return body.results().stream()
				.map(res -> transformTmdbFilmToDvdThequeFilm(null, res, alreadyInDvdtheque, false))
				.filter(Objects::nonNull)
				.toList();
	}
	public Film processRetrieveCritiquePresse(Long id, BiConsumer<Film,Set<CritiquePresseDto>> consumer, Film updatedFilm) {
		Film film = filmSaveService.findFilm(id);
		if(film != null) {
			if(film.getAllocineFicheFilmId() != null) {
				ResponseEntity<FicheFilmDto> ficheFilmDtoResponse = restTemplate.exchange(
						environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
								+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_ID) + "?id=" + film.getAllocineFicheFilmId(),
						HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                        });
				if(ficheFilmDtoResponse.getBody() != null) {
					Set<CritiquePresseDto> cpDtoSet = ficheFilmDtoResponse.getBody().getCritiquePresse();
					consumer.accept(film,cpDtoSet);
				}
			}else {
				ResponseEntity<List<FicheFilmDto>> ficheFilmDtoResponse = restTemplate.exchange(
						environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
								+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE) + "?title=" + film.getTitre()+"&titleO="+ film.getTitreO(),
						HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                        });
				if(ficheFilmDtoResponse.getBody() != null && CollectionUtils.isNotEmpty(ficheFilmDtoResponse.getBody())) {
					Set<CritiquePresseDto> cpDtoSet = ficheFilmDtoResponse.getBody().getFirst().getCritiquePresse();
					consumer.accept(film,cpDtoSet);
				}
			}
		}
		if(updatedFilm == null) {
			return film;
		}
		return updatedFilm;
	}

	public Film replaceFilm(Film film,Long tmdbId) {
		Results results = restTemplate.getForObject(
				environment.getRequiredProperty(TMDB_SERVICE_URL)
						+ environment.getRequiredProperty(TMDB_SERVICE_RESULTS) + "?tmdbId=" + tmdbId,
				Results.class);
		Film toUpdateFilm = transformTmdbFilmToDvdThequeFilm(film, results, new HashSet<>(), true);
		if (toUpdateFilm != null) {
			toUpdateFilm.setOrigine(film.getOrigine());
			filmSaveService.updateFilm(toUpdateFilm);
			return toUpdateFilm;
		}
		return toUpdateFilm;
	}

	public Film updateFilm(Film film,Long id){
		Film mergedFilm = filmSaveService.updateFilm(film);
        return processRetrieveCritiquePresse(id, (f, set) -> addCritiquePresseToFilm(set, mergedFilm),mergedFilm);
	}

	public void addCritiquePresseToFilm(Set<CritiquePresseDto> cpDtoSet,Film film) {
		if(CollectionUtils.isNotEmpty(cpDtoSet)) {
			for(CritiquePresseDto cto : cpDtoSet) {
				CritiquePresse cp = new CritiquePresse();
				cp.setAuthor(cto.getAuthor());
				cp.setBody(cto.getBody());
				cp.setRating(cto.getRating());
				cp.setNewsSource(cto.getNewsSource());
				film.addCritiquePresse(cp);
			}
			film.getCritiquePresse().sort(Comparator.comparing(CritiquePresse::getRating));
		}
	}

	public Film retrieveFilmImage(Long id){
		Film film = filmSaveService.findFilm(id);

		Results results = restTemplate.getForObject(
				environment.getRequiredProperty(TMDB_SERVICE_URL) + environment.getRequiredProperty(TMDB_SERVICE_RESULTS) + "?tmdbId=" + film.getTmdbId(), Results.class);
		film.setPosterPath(
				environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL) + results.poster_path());
		return filmSaveService.updateFilm(film);
	}

	public void retrieveAllFilmImages(){
		Page<Film> films = paginatedSearch("", null, null, "");
		for(int i = 0 ; i<films.getContent().size();i++) {
			Film film = films.getContent().get(i);
			Boolean posterExists = restTemplate.getForObject(
					environment.getRequiredProperty(TMDB_SERVICE_URL) + "?posterPath=" + film.getPosterPath(),
					Boolean.class);
			if (Boolean.FALSE.equals(posterExists)) {
				Results results = restTemplate.getForObject(
						environment.getRequiredProperty(TMDB_SERVICE_URL) + "?tmdbId=" + film.getTmdbId(),
						Results.class);
				film.setPosterPath(environment.getRequiredProperty(TmdbServiceCommon.TMDB_POSTER_PATH_URL)
						+ results.poster_path());
			}
			filmSaveService.updateFilm(film);
		}
	}

	public FilmDto saveProcessedFilm(Film film) {
		ResultsByTmdbId results = restTemplate.getForObject(environment.getRequiredProperty(TMDB_SERVICE_URL)
				+ environment.getRequiredProperty(FilmController.TMDB_SERVICE_RESULTS) + "?tmdbId="
				+ film.getTmdbId(), ResultsByTmdbId.class);
		if (results != null) {
			Results res = transformTmdbFilmToResults(results);
			Film filmToSave = transformTmdbFilmToDvdThequeFilm(film,
					res,
                    new HashSet<>(),
					true);
			if (filmToSave != null) {
				filmToSave.setId(null);
				filmToSave.setOrigine(film.getOrigine());
				if(film.getDateInsertion() != null) {
					filmToSave.setDateInsertion(film.getDateInsertion());
				}else {
					filmToSave.setDateInsertion(LocalDate.now());
				}
				filmToSave.setVu(film.isVu());
				filmToSave.setDateVue(film.getDateVue());
				filmToSave.setDateSortieDvd(film.getDateSortieDvd());
				Film savedFilm = filmSaveService.saveNewFilm(filmToSave);
				logger.info(filmToSave.toString());
				filmToSave.setId(savedFilm.getId());
				return FilmDto.of(filmToSave);
			}
		}
		throw new FilmNotFoundException("Film with tmdbId "+film.getTmdbId()+" not found");
	}

	public void importFilmList(MultipartFile file) throws IOException {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		// Transform MultipartFile en Resource
		body.add("file", new ByteArrayResource(file.getBytes()) {
			@Override
			public String getFilename() {
				return file.getOriginalFilename();
			}
		});

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(
				environment.getRequiredProperty(DVDTHEQUE_BATCH_SERVICE_URL)
						+ environment.getRequiredProperty(DVDTHEQUE_BATCH_SERVICE_IMPORT),
				HttpMethod.POST,
				requestEntity,
				String.class
		);

		logger.info(response.getBody());
	}
}
