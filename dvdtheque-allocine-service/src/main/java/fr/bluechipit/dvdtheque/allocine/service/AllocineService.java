package fr.bluechipit.dvdtheque.allocine.service;

import com.google.common.util.concurrent.RateLimiter;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.domain.Page;
import fr.bluechipit.dvdtheque.allocine.dto.FicheFilmRec;
import fr.bluechipit.dvdtheque.allocine.dto.SearchResponseDTO;
import fr.bluechipit.dvdtheque.allocine.repository.FicheFilmRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import specifications.filter.PageRequestBuilder;
import specifications.filter.SpecificationsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = {"ficheFilms","ficheFilmsByTitle"})
@ComponentScan({"fr.fredos.dvdtheque.common.specifications.filter","specifications.filter"})
public class AllocineService {
	protected Logger logger = LoggerFactory.getLogger(AllocineService.class);

	private final FicheFilmRepository ficheFilmRepository;
	private final Environment environment;
	private final HazelcastInstance instance;
	private final SpecificationsBuilder<FicheFilm> builder;
	private final ExecutorService fixedThreadPool;
	private final RestTemplate restTemplate;

	@Value("${fichefilm.parsing.page}")
	private int nbParsedPage;

	@Value("${scraping.batch.size:50}")
	private int batchSize;

	@Value("${scraping.rate.limit.ms:1000}")
	private long rateLimitMs;

	private IMap<Integer, FicheFilm> mapFicheFilms;
	private IMap<String, List<FicheFilm>> mapFicheFilmsByTitle;
	private AtomicInteger pageNum;
	private RateLimiter rateLimiter;

	// HTML Selectors - Constants
	private static final String AHREF = "a[href]";
	private static final String HREF = "href";
	private static final String SPAN = "span";
	private static final String P = "p";
	private static final String H2 = "h2";
	private static final String DIV = "div";
	private static final String SECTION = "section";
	private static final String DIV_EVAL_HOLDER = "div.eval-holder";
	private static final String DIV_REVIEWS_PRESS_COMMENT = "div.reviews-press-comment";

	// Rating selectors
	private static final String[] RATING_SELECTORS = {
			"div.rating-mdl.n10.stareval-stars",
			"div.rating-mdl.n20.stareval-stars",
			"div.rating-mdl.n30.stareval-stars",
			"div.rating-mdl.n40.stareval-stars",
			"div.rating-mdl.n50.stareval-stars"
	};

	// URL patterns
	private static final String BASE_URL = "https://www.allocine.fr";
	private static final String LIST_FILM_URL = "/films/";
	private static final String FILM_DELIMITER = "/film/fichefilm_gen_cfilm=";
	private static final String SEARCH_PAGE_DELIMITER = "?page=";
	private static final String CRITIQUE_PRESSE_FILM_BASE_URL = "/film/fichefilm-";
	private static final String CRITIQUE_PRESSE_FILM_END_URL = "/critiques/presse/";
	private static final String AUTO_COMPLETED_URL = "/_/autocomplete/";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36";

	// Retry configuration
	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final long RETRY_DELAY_MS = 2000;

	@Autowired
	public AllocineService(
			FicheFilmRepository ficheFilmRepository,
			HazelcastInstance instance,
			Environment environment,
			SpecificationsBuilder<FicheFilm> builder,
			ExecutorService fixedThreadPool,
			RestTemplate restTemplate) {
		this.ficheFilmRepository = ficheFilmRepository;
		this.instance = instance;
		this.environment = environment;
		this.builder = builder;
		this.fixedThreadPool = fixedThreadPool;
		this.rateLimiter = RateLimiter.create(1000.0 / rateLimitMs); // permits per second
		this.restTemplate = restTemplate;
		this.init();
	}

	public void init() {
		mapFicheFilms = instance.getMap("ficheFilms");
		mapFicheFilmsByTitle = instance.getMap("ficheFilmsByTitle");
		logger.info("AllocineService initialized with cache maps");
	}

	// ==================== Public API Methods ====================

	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<FicheFilmRec> paginatedSearch(String query,
																			 Integer offset,
																			 Integer limit,
																			 String sort){
		PageRequest pageRequest = buildDefaultPageRequest(offset, limit, sort);
		org.springframework.data.domain.Page<FicheFilm> page = StringUtils.isEmpty(query)
				? ficheFilmRepository.findAll(pageRequest)
				: ficheFilmRepository.findAll(builder.with(query).build(), pageRequest);

		List<FicheFilmRec> records = page.getContent().stream()
				.map(FicheFilmRec::fromEntity)
				.collect(Collectors.toList());

		return new PageImpl<>(records, pageRequest, page.getTotalElements());
	}

	public List<FicheFilm> retrieveAllFicheFilm() {
		return ficheFilmRepository.findAll();
	}

	public Optional<FicheFilm> retrieveFicheFilm(int id) {
		return ficheFilmRepository.findById(id);
	}

	public List<FicheFilm> retrieveFicheFilmByTitle(String title) {
		if (StringUtils.isEmpty(title)) {
			return Collections.emptyList();
		}

		String normalizedTitle = StringUtils.upperCase(title);
		Optional<List<FicheFilm>> cached = findInCacheByFicheFilmTitle(normalizedTitle);

		if (cached.isPresent()) {
			logger.debug("Cache hit for title: {}", title);
			return cached.get();
		}

		List<FicheFilm> results = new ArrayList<>(
				new HashSet<>(ficheFilmRepository.findByTitle(title))
		);

		if (!results.isEmpty()) {
			mapFicheFilmsByTitle.putIfAbsent(normalizedTitle, results);
		}

		return results;
	}

	public Optional<FicheFilm> findByFicheFilmId(Integer ficheFilmId) {
		if (ficheFilmId == null) {
			return Optional.empty();
		}

		Optional<FicheFilm> cached = findInCacheByFicheFilmId(ficheFilmId);
		if (cached.isPresent()) {
			return cached;
		}

		FicheFilm ficheFilm = ficheFilmRepository.findByFicheFilmId(ficheFilmId);
		if (ficheFilm != null) {
			mapFicheFilms.putIfAbsent(ficheFilmId, ficheFilm);
		}

		return Optional.ofNullable(ficheFilm);
	}

	// ==================== Scraping Methods ====================

	public void scrapAllAllocineFicheFilm() {
		logger.info("Starting sequential scraping of {} pages", nbParsedPage);

		for (int pageNumber = 1; pageNumber <= nbParsedPage; pageNumber++) {
			try {
				Page page = new Page(pageNumber);
				processPage(page);
			} catch (Exception e) {
				logger.error("Failed to process page {}", pageNumber, e);
			}
		}

		logger.info("Sequential scraping completed");
	}

	public ScrapingResult scrapAllAllocineFicheFilmMultithreaded() {
		logger.info("Starting multithreaded scraping of {} pages", nbParsedPage);

		pageNum = new AtomicInteger(0);
		List<Future<PageResult>> futures = new ArrayList<>();

		for (int i = 0; i < nbParsedPage; i++) {
			Callable<PageResult> task = this::processNextPage;
			futures.add(fixedThreadPool.submit(task));
		}

		ScrapingResult result = waitForCompletion(futures);
		logger.info("Multithreaded scraping completed: {}", result);

		return result;
	}

	// ==================== Private Processing Methods ====================

	private PageResult processNextPage() {
		Page page = incrementAndGetPage();

		try {
			return processPage(page);
		} catch (Exception e) {
			logger.error("Error processing page {}", page.getNumPage(), e);
			return new PageResult(page.getNumPage(), 0, false, e.getMessage());
		}
	}

	private PageResult processPage(Page page) {
		logger.debug("Processing page {}", page.getNumPage());

		Set<FicheFilm> ficheFilms = retrieveAllFicheFilmFromPage(page);

		if (CollectionUtils.isEmpty(ficheFilms)) {
			logger.warn("No films found on page {}", page.getNumPage());
			return new PageResult(page.getNumPage(), 0, true, null);
		}

		int savedCount = processCritiquePress(ficheFilms);
		logger.info("Page {} processed: {} films found, {} saved",
				page.getNumPage(), ficheFilms.size(), savedCount);

		return new PageResult(page.getNumPage(), savedCount, true, null);
	}

	private int processCritiquePress(Set<FicheFilm> ficheFilms) {
		List<FicheFilm> toSave = new ArrayList<>();

		for (FicheFilm ficheFilm : ficheFilms) {
			try {
				if (shouldProcessFicheFilm(ficheFilm)) {
					Map<Integer, CritiquePresse> critiques = retrieveCritiquePresseMap(ficheFilm);

					if (MapUtils.isNotEmpty(critiques)) {
						toSave.add(ficheFilm);

						// Save to cache
						mapFicheFilms.putIfAbsent(ficheFilm.getAllocineFilmId(), ficheFilm);

						// Batch save when reaching batch size
						if (toSave.size() >= batchSize) {
							saveFicheFilmBatch(toSave);
							toSave.clear();
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error processing ficheFilm {}: {}",
						ficheFilm.getAllocineFilmId(), e.getMessage());
			}
		}

		// Save remaining items
		if (!toSave.isEmpty()) {
			saveFicheFilmBatch(toSave);
		}

		return toSave.size();
	}

	private boolean shouldProcessFicheFilm(FicheFilm ficheFilm) {
		if (ficheFilm == null || ficheFilm.getAllocineFilmId() == 0) {
			return false;
		}

		// Check cache first
		if (findInCacheByFicheFilmId(ficheFilm.getAllocineFilmId()).isPresent()) {
			logger.debug("FicheFilm {} already in cache, skipping", ficheFilm.getAllocineFilmId());
			return false;
		}

		// Check database
		Optional<FicheFilm> existing = findByFicheFilmId(ficheFilm.getAllocineFilmId());
		return existing.isEmpty();
	}

	private Map<Integer, CritiquePresse> retrieveCritiquePresseMap(FicheFilm ficheFilm) {
		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				rateLimiter.acquire(); // Rate limiting
				return fetchCritiques(ficheFilm);
			} catch (IOException e) {
				logger.warn("Attempt {}/{} failed for film {}: {}",
						attempt, MAX_RETRY_ATTEMPTS, ficheFilm.getAllocineFilmId(), e.getMessage());

				if (attempt < MAX_RETRY_ATTEMPTS) {
					sleep(RETRY_DELAY_MS * attempt);
				}
			}
		}

		logger.error("All retry attempts exhausted for film {}", ficheFilm.getAllocineFilmId());
		return Collections.emptyMap();
	}

	private Map<Integer, CritiquePresse> fetchCritiques(FicheFilm ficheFilm) throws IOException {
		Document document = Jsoup.connect(ficheFilm.getUrl())
				.userAgent(USER_AGENT)
				.timeout(10000)
				.get();

		Map<Integer, CritiquePresse> critiquesMap = new HashMap<>();
		Elements reviewSections = document.select(DIV_REVIEWS_PRESS_COMMENT);

		for (Element reviewSection : reviewSections) {
			int index = 0;

			// Extract news sources
			Map<Integer, CritiquePresse> tempMap = extractNewsSources(reviewSection, ficheFilm);

			// Extract bodies
			extractBodies(reviewSection, tempMap);

			// Extract ratings and authors
			extractRatingsAndAuthors(reviewSection, tempMap);

			critiquesMap.putAll(tempMap);
		}

		return critiquesMap;
	}

	private Map<Integer, CritiquePresse> extractNewsSources(Element reviewSection, FicheFilm ficheFilm) {
		Map<Integer, CritiquePresse> map = new HashMap<>();
		Elements headers = reviewSection.select(H2 + " " + SPAN);
		int index = 0;

		for (Element header : headers) {
			String newsSource = header.text();
			if (StringUtils.isNotEmpty(newsSource)) {
				CritiquePresse critique = new CritiquePresse();
				critique.setNewsSource(newsSource);
				critique.setBody("...");
				critique.setAuthor("...");
				critique.setRating(0.0);
				critique.setFicheFilm(ficheFilm);

				map.put(index++, critique);
				ficheFilm.addCritiquePresse(critique);
			}
		}

		return map;
	}

	private void extractBodies(Element reviewSection, Map<Integer, CritiquePresse> critiquesMap) {
		Elements paragraphs = reviewSection.select(P);
		int index = 0;

		for (Element paragraph : paragraphs) {
			String body = paragraph.text();
			if (StringUtils.isNotEmpty(body)) {
				CritiquePresse critique = critiquesMap.get(index++);
				if (critique != null) {
					critique.setBody(body);
				}
			}
		}
	}

	private void extractRatingsAndAuthors(Element reviewSection, Map<Integer, CritiquePresse> critiquesMap) {
		Elements evalHolders = reviewSection.select(DIV_EVAL_HOLDER);
		int index = 0;

		for (Element evalHolder : evalHolders) {
			CritiquePresse critique = critiquesMap.get(index++);
			if (critique == null) continue;

			// Extract author
			String author = evalHolder.select(DIV_EVAL_HOLDER).text();
			if (StringUtils.isNotEmpty(author)) {
				critique.setAuthor(author);
			}

			// Extract rating
			Double rating = extractRating(evalHolder);
			critique.setRating(rating);

			// Ensure body is not empty
			if (StringUtils.isEmpty(critique.getBody())) {
				critique.setBody("...");
			}

			logger.debug("Extracted critique: {}", critique);
		}
	}

	private Double extractRating(Element element) {
		for (int i = RATING_SELECTORS.length - 1; i >= 0; i--) {
			if (element.select(RATING_SELECTORS[i]).size() > 0) {
				return (double) (i + 1);
			}
		}
		return 0.0;
	}

	public Optional<FicheFilm> extractFicheFilm(String title) {
		String url = BASE_URL + AUTO_COMPLETED_URL + title;
		SearchResponseDTO response = restTemplate.getForEntity(url, SearchResponseDTO.class).getBody();
        if(response != null){
			var res = response.results().stream().filter(f-> StringUtils.equalsIgnoreCase(f.label(), title)).findFirst();
			if(res.isPresent()){
				var id = res.get().data().id();
				var ff = new FicheFilm(title,
						Integer.valueOf(id),
						"https://www.allocine.fr/film/fichefilm-"+id+"/critiques/presse/", 1000);
				int saved = processCritiquePress(Set.of(ff));
				logger.info("Saved {} critiques for film {}", saved, title);
				return Optional.of(ff);
			}
		}
		return Optional.empty();
	}
	private Set<FicheFilm> retrieveAllFicheFilmFromPage(Page page) {
		try {
			rateLimiter.acquire();

			String url = page.getNumPage() == 1
					? BASE_URL + LIST_FILM_URL
					: BASE_URL + LIST_FILM_URL + SEARCH_PAGE_DELIMITER + page.getNumPage();

			Document document = Jsoup.connect(url)
					.userAgent(USER_AGENT)
					.timeout(10000)
					.get();

			return extractFicheFilmsFromDocument(document, page.getNumPage());

		} catch (IOException e) {
			logger.error("Failed to retrieve films from page {}", page.getNumPage(), e);
			return Collections.emptySet();
		}
	}

	private Set<FicheFilm> extractFicheFilmsFromDocument(Document document, int pageNumber) {
		Set<FicheFilm> ficheFilms = new HashSet<>();
		Elements links = document.select(AHREF);

		for (Element link : links) {
			String href = link.attr(HREF);

			if (StringUtils.contains(href, FILM_DELIMITER)) {
				try {
					FicheFilm ficheFilm = createFicheFilmFromLink(link, href, pageNumber);
					ficheFilms.add(ficheFilm);
				} catch (Exception e) {
					logger.warn("Failed to parse film from link: {}", href, e);
				}
			}
		}

		logger.debug("Extracted {} films from page {}", ficheFilms.size(), pageNumber);
		return ficheFilms;
	}

	private FicheFilm createFicheFilmFromLink(Element link, String href, int pageNumber) {
		String filmTempId = StringUtils.substringAfter(href, FILM_DELIMITER);
		String filmId = StringUtils.substringBefore(filmTempId, ".html");
		String url = BASE_URL + CRITIQUE_PRESSE_FILM_BASE_URL + filmId + CRITIQUE_PRESSE_FILM_END_URL;

		logger.debug("Created FicheFilm: title={}, id={}, url={}", link.text(), filmId, url);

		return new FicheFilm(link.text(), Integer.valueOf(filmId), url, pageNumber);
	}

	// ==================== Database Operations ====================

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public FicheFilm saveFicheFilm(FicheFilm ficheFilm) {
		try {
			FicheFilm saved = ficheFilmRepository.save(ficheFilm);
			logger.debug("Saved FicheFilm: {}", saved.getAllocineFilmId());
			return saved;
		} catch (Exception e) {
			logger.error("Error saving FicheFilm {}", ficheFilm.getAllocineFilmId(), e);
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveFicheFilmBatch(List<FicheFilm> ficheFilms) {
		if (CollectionUtils.isEmpty(ficheFilms)) {
			return;
		}

		try {
			ficheFilmRepository.saveAll(ficheFilms);
			logger.info("Saved batch of {} FicheFilms", ficheFilms.size());
		} catch (Exception e) {
			logger.error("Error saving FicheFilm batch of size {}", ficheFilms.size(), e);
		}
	}

	// ==================== Helper Methods ====================

	private PageRequest buildDefaultPageRequest(Integer offset, Integer limit, String sort) {
		int actualLimit = limit != null ? limit : 50;
		int actualOffset = offset != null ? offset : 1;
		String actualSort = StringUtils.isNotEmpty(sort) ? sort : "-creationDate";

		return PageRequestBuilder.getPageRequest(actualLimit, actualOffset, actualSort);
	}

	private synchronized Page incrementAndGetPage() {
		int currentPage = pageNum.incrementAndGet();
		Page page = new Page(currentPage);
		page.setNumPage(currentPage);
		return page;
	}

	private ScrapingResult waitForCompletion(List<Future<PageResult>> futures) {
		int successCount = 0;
		int failureCount = 0;
		int totalSaved = 0;

		for (Future<PageResult> future : futures) {
			try {
				PageResult result = future.get();
				if (result.isSuccess()) {
					successCount++;
					totalSaved += result.getSavedCount();
				} else {
					failureCount++;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.error("Scraping task interrupted", e);
				failureCount++;
			} catch (ExecutionException e) {
				logger.error("Scraping task failed", e);
				failureCount++;
			}
		}

		return new ScrapingResult(successCount, failureCount, totalSaved);
	}

	public Optional<FicheFilm> findInCacheByFicheFilmId(Integer ficheFilmId) {
		FicheFilm ficheFilm = mapFicheFilms.get(ficheFilmId);
		return Optional.ofNullable(ficheFilm);
	}

	public Optional<List<FicheFilm>> findInCacheByFicheFilmTitle(String title) {
		List<FicheFilm> ficheFilms = mapFicheFilmsByTitle.get(title);
		return Optional.ofNullable(ficheFilms);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("Sleep interrupted", e);
		}
	}

	// ==================== Inner Classes ====================

	public static class PageResult {
		private final int pageNumber;
		private final int savedCount;
		private final boolean success;
		private final String errorMessage;

		public PageResult(int pageNumber, int savedCount, boolean success, String errorMessage) {
			this.pageNumber = pageNumber;
			this.savedCount = savedCount;
			this.success = success;
			this.errorMessage = errorMessage;
		}

		public int getPageNumber() { return pageNumber; }
		public int getSavedCount() { return savedCount; }
		public boolean isSuccess() { return success; }
		public String getErrorMessage() { return errorMessage; }
	}

	public static class ScrapingResult {
		private final int successfulPages;
		private final int failedPages;
		private final int totalSaved;

		public ScrapingResult(int successfulPages, int failedPages, int totalSaved) {
			this.successfulPages = successfulPages;
			this.failedPages = failedPages;
			this.totalSaved = totalSaved;
		}

		public int getSuccessfulPages() { return successfulPages; }
		public int getFailedPages() { return failedPages; }
		public int getTotalSaved() { return totalSaved; }

		@Override
		public String toString() {
			return String.format("ScrapingResult{successful=%d, failed=%d, totalSaved=%d}",
					successfulPages, failedPages, totalSaved);
		}
	}
}
