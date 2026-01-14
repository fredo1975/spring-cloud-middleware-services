package fr.bluechipit.dvdtheque.service.impl;

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
import fr.bluechipit.dvdtheque.dao.repository.DvdDao;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import specifications.filter.PageRequestBuilder;
import tmdb.model.*;
import utils.DateUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static fr.bluechipit.dvdtheque.controller.FilmController.*;

@Service("filmService")
@CacheConfig(cacheNames = "films")
public class FilmService {
	protected Logger logger = LoggerFactory.getLogger(FilmService.class);

	private static String NB_ACTEURS = "batch.save.nb.acteurs";
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
	@Autowired
	private SpecificationsBuilder<Film> builder;

	public FilmService(FilmDao filmDao, DvdDao dvdDao, GenreDao genreDao, PersonneService personneService, HazelcastInstance instance,ExcelFilmHandler excelFilmHandler,
					   RestTemplate restTemplate,
					   Environment environment,
					   FilmSaveService filmSaveService) {
		this.filmDao = filmDao;
		this.genreDao = genreDao;
		this.personneService = personneService;
		this.instance = instance;
		this.excelFilmHandler = excelFilmHandler;
		this.restTemplate = restTemplate;
		this.environment = environment;
		this.filmSaveService = filmSaveService;
		this.init();
	}

	public void init() {
		mapGenres = instance.getMap(CACHE_GENRE);
	}

	@Transactional(readOnly = true)
	public List<FilmDto> getAllFilmDtos() {
		List<Film> filmList = null;
		List<FilmDto> filmDtoList = new ArrayList<>();
		try {
			filmList = filmDao.findAll();
			if (!CollectionUtils.isEmpty(filmList)) {
				logger.debug("####################   filmList.size()=" + filmList.size());
				for (Film film : filmList) {
					FilmDto filmDto = FilmDto.toDto(film);
					filmDtoList.add(filmDto);
				}
			}
		} catch (Exception e) {
			logger.error(e.getCause().getMessage());
		}
		filmDtoList.sort(Comparator.comparing(FilmDto::getTitre));
		return filmDtoList;
	}

	@Transactional(readOnly = true, noRollbackFor = { org.springframework.dao.EmptyResultDataAccessException.class })
	public Film findFilmByTitreWithoutSpecialsCharacters(final String titre) {
		return filmDao.findFilmByTitreWithoutSpecialsCharacters(titre);
	}
	@Transactional(readOnly = true)
	public Page<Film> findAllFilmByOrigine(final FilmOrigine origine) {
		var page = buildDefaultPageRequest(1, 10, "-dateSortie");
		return filmDao.findAll(builder.with("origine:eq:"+origine+":AND,").build(), page);
	}

	@Transactional(readOnly = true)
	public Page<Film> findAllFilmByDvdFormat(final DvdFormat format) {
		var page = buildDefaultPageRequest(1, 10, "-dateSortie");
		return filmDao.findAll(builder.with("dvd.format:eq:"+format+":AND,").build(), page);
	}

	@Transactional(readOnly = true)
	public Genre findGenre(int tmdbId) {
		return genreDao.findGenreByTmdbId(tmdbId);
	}

	private static int retrieveYearFromReleaseDate(final Date relDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(relDate);
		return cal.get(Calendar.YEAR);
	}

	/**
	 * create a dvdtheque Film based on a TMBD film
	 *
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
				Genre _g = this.mapGenres.get(g.id());
                logger.error("genre " + g.name() + " not found in loaded genres");
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

	public List<Crew> retrieveTmdbDirectors(final Credits credits) {
		return credits.getCrew().stream().filter(cred -> cred.getJob().equalsIgnoreCase("Director"))
				.collect(Collectors.toList());
	}

	private void retrieveAndSetCredits(final boolean persistPersonne, final Results results,
									   final Film transformedfilm) {
		Credits credits = restTemplate.getForObject(
				environment.getRequiredProperty(TMDB_SERVICE_URL)
						+ environment.getRequiredProperty(TMDB_SERVICE_CREDITS) + "?tmdbId=" + results.getId(),
				Credits.class);
        assert credits != null;
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
	public Optional<Film> saveFilm(Long tmdbId, String origine) throws ParseException {
		Film filmToSave = null;
        FilmOrigine filmOrigine = FilmOrigine.valueOf(origine);
        if (checkIfTmdbFilmExists(tmdbId)) {
            return Optional.empty();
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
                    Dvd dvd = buildDvd(filmToSave.getAnnee(),
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
            return Optional.empty();
        }
        return Optional.of(filmToSave);
    }


	@Transactional(readOnly = false)
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
			l.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));
			return l;
		}
		logger.debug("no genres find");
		List<Genre> e = this.genreDao.findAll();
		logger.debug("genres size: " + e.size());
		if(CollectionUtils.isNotEmpty(e)) {
			e.parallelStream().forEach(it -> {
				mapGenres.putIfAbsent(it.getId(), it);
			});
		}
		return e;
	}


	@Transactional(readOnly = false)
	public void cleanAllFilms() {
		filmDao.deleteAll();
		genreDao.deleteAll();
		mapGenres.clear();
		personneService.cleanAllPersonnes();
	}


	@Transactional(readOnly = true)
	public List<Film> getAllRippedFilms() {
		return filmDao.getAllRippedFilms();
	}


	public byte[] exportFilmList(String origine) throws IOException {
		List<Film> list = new ArrayList<>();
		FilmOrigine filmOrigine = FilmOrigine.valueOf(origine);
		//Page<Film> films;
		if(filmOrigine == FilmOrigine.TOUS) {
			Page<Film> films = paginatedSarch("",1, null, "");
			list.addAll(films.getContent());
			while(films.hasNext()) {
				Pageable p = films.nextPageable();
				films = paginatedSarch("",p.getPageNumber()+1, null, "");
				list.addAll(films.getContent());
			}
		}else {
			Page<Film> films = paginatedSarch("origine:eq:"+filmOrigine+":AND",1, null, "");
			list.addAll(films.getContent());
			while(films.hasNext()) {
				Pageable p = films.nextPageable();
				films = paginatedSarch("origine:eq:"+filmOrigine+":AND",p.getPageNumber()+1, null, "");
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
		String fileName = "ListeDVDSearch" + "-" + localDate.getSecond() + ".xlsx";
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
		Integer limitToSet;
		Integer offsetToSet;
		String sortToSet;
		if(limit == null) {
			limitToSet = Integer.valueOf(50);
		}else {
			limitToSet = limit;
		}
		if(offset == null) {
			offsetToSet = Integer.valueOf(1);
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

	@Transactional(readOnly = true)
	public Page<Film> paginatedSarch(String query,
			Integer offset,
			Integer limit,
			String sort){
		var page = buildDefaultPageRequest(offset, limit, sort);
		if(StringUtils.isEmpty(query)) {
			return filmDao.findAll(page);
		}
        return filmDao.findAll(builder.with(query).build(), page);
	}

	public List<Film> findFilmByOrigine(final FilmOrigine origine){
		return filmDao.findFilmByOrigine(origine);
	}
	

	@Transactional(readOnly = false)
	public void removeFilm(Film film) {
		//film = mapFilms.get(film.getId());
		Optional<Film> filmOpt = filmDao.findById(film.getId());
		if(filmOpt.isPresent()) {
			filmDao.delete(filmOpt.get());
			//mapFilms.remove(filmOpt.get().getId());
		}
	}

	public static void saveImage(final String imageUrl, final String destinationFile) throws IOException {
		URL url = new URL(imageUrl);
		InputStream is = url.openStream();
		OutputStream os = new FileOutputStream(destinationFile);
		byte[] b = new byte[2048];
		int length;
		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}
		is.close();
		os.close();
	}


	@Transactional(readOnly = true)
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

	@Transactional(readOnly = true)
	public Dvd buildDvd(final Integer annee, final Integer zone, final String edition, final Date ripDate,
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
			dvd.setDateRip(clearDate(ripDate));
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
		return filmDao.checkIfTmdbFilmExists(tmdbId).equals(Integer.valueOf(1))?Boolean.TRUE:Boolean.FALSE;
	}

	public Set<CritiquePresseDto> findAllCritiquePresseByAllocineFilmById(Integer id){
		ResponseEntity<FicheFilmDto> ficheFilmDtoResponse = restTemplate.exchange(
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
						+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_ID) + "?id=" + id,
				HttpMethod.GET, null, new ParameterizedTypeReference<FicheFilmDto>() {});
		if(ficheFilmDtoResponse.getBody() != null) {
			return ficheFilmDtoResponse.getBody().getCritiquePresse();
		}
		return Collections.emptySet();
	}

	public List<FicheFilmDto> findAllCritiquePresseByAllocineFilmByTitle(String title){
		ResponseEntity<List<FicheFilmDto>> ficheFilmDtoResponse = restTemplate.exchange(
				environment.getRequiredProperty(ALLOCINE_SERVICE_URL)
						+ environment.getRequiredProperty(ALLOCINE_SERVICE_BY_TITLE) + "?title=" + title+"&titleO="+title,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<FicheFilmDto>>() {});
		if(ficheFilmDtoResponse.getBody() != null) {
			return ficheFilmDtoResponse.getBody();
		}
		return Collections.emptyList();
	}

	public List<Film> findTmdbFilmByTitre(String titre,Integer page) throws ParseException {
		List<Film> films = null;
		String url = page == null ? environment.getRequiredProperty(TMDB_SERVICE_URL)
				+ environment.getRequiredProperty(TMDB_SERVICE_BY_TITLE) + "?title=" + titre : environment.getRequiredProperty(TMDB_SERVICE_URL)
				+ environment.getRequiredProperty(TMDB_SERVICE_BY_TITLE_BY_PAGE) + "?title=" + titre+ "&page="+page;
		ResponseEntity<List<Results>> resultsResponse = restTemplate.exchange(url,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Results>>() {});
		if (CollectionUtils.isNotEmpty(resultsResponse.getBody())) {
			List<Results> results = resultsResponse.getBody();
			films = new ArrayList<>(results.size());
			Set<Long> tmdbIds = results.stream().map(r -> r.getId()).collect(Collectors.toSet());
			Set<Long> tmdbFilmAlreadyInDvdthequeSet = findAllTmdbFilms(tmdbIds);
			for (Results res : results) {
				Film transformedFilm = transformTmdbFilmToDvdThequeFilm(null, res, tmdbFilmAlreadyInDvdthequeSet,
						false);
				if (transformedFilm != null) {
					films.add(transformedFilm);
				}
			}
		}
		return films;
	}
	public Film processRetrieveCritiquePresse(Long id, BiConsumer<Film,Set<CritiquePresseDto>> consumer, Film updatedFilm) {
		Film film = filmSaveService.findFilm(id);
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

	public Film replaceFilm(Film film,Long tmdbId) throws ParseException {
		Film filmOptional = filmSaveService.findFilm(film.getId());
		Results results = restTemplate.getForObject(
				environment.getRequiredProperty(TMDB_SERVICE_URL)
						+ environment.getRequiredProperty(TMDB_SERVICE_RESULTS) + "?tmdbId=" + tmdbId,
				Results.class);
		Film toUpdateFilm = transformTmdbFilmToDvdThequeFilm(film, results, new HashSet<Long>(), true);
		if (toUpdateFilm != null) {
			toUpdateFilm.setOrigine(film.getOrigine());
			filmSaveService.updateFilm(toUpdateFilm);
			return toUpdateFilm;
		}
		return toUpdateFilm;
	}

	public Film updateFilm(Film film,Long id){
		Film mergedFilm = filmSaveService.updateFilm(film);
        return processRetrieveCritiquePresse(id, (f, set) -> {
            addCritiquePresseToFilm(set, mergedFilm);
        },mergedFilm);
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
			film.getCritiquePresse().sort(new Comparator<CritiquePresse>() {
				@Override
				public int compare(CritiquePresse o1, CritiquePresse o2) {
					return o1.getRating().compareTo(o2.getRating());
				}
			});
		}
	}
}
