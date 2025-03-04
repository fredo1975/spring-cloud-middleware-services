package fr.bluechipit.dvdtheque.batch.processor;

import enums.FilmOrigine;
import enums.JmsStatus;
import fr.bluechipit.dvdtheque.batch.format.FilmCsvImportFormat;
import jms.model.JmsStatusMessage;
import model.Dvd;
import model.DvdBuilder;
import model.Film;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import jakarta.jms.Topic;

public class FilmProcessor implements ItemProcessor<FilmCsvImportFormat, Film> {
	protected Logger logger = LoggerFactory.getLogger(FilmProcessor.class);
	
	@Autowired
    Environment 													environment;
	@Autowired
    private JmsTemplate 											jmsTemplate;
	@Autowired
    private Topic 													topic;
	@Autowired
    private RestTemplate 											restTemplate;
	@Autowired
	AuthorizedClientServiceOAuth2AuthorizedClientManager 			authorizedClientServiceAndManager;
	
	public FilmProcessor() {
    }
	private static String RIPPEDFLAGTASKLET_FROM_FILE="rippedFlagTasklet.from.file";
	public static String TMDB_SERVICE_URL="tmdb-service.url";
	public static String DVDTHEQUE_SERVICE_URL="dvdtheque-service.url";
	public static String TMDB_SERVICE_BY_TMDBID="tmdb-service.get-results";
	public static String DVDTHEQUE_SERVICE_BUILD_DVD="dvdtheque-service.buildDvd";
	public static String DVDTHEQUE_SERVICE_TRANSFORM_BY_TMDBID="dvdtheque-service.transformTmdbFilmToDvdThequeFilm";
	
	@Override
	public Film process(FilmCsvImportFormat item) throws Exception {
		StopWatch watch = new StopWatch();
		watch.start();
		Film filmTemp = new Film ();
		filmTemp.setTmdbId(item.getTmdbId());
		filmTemp.setTitre(item.getTitre());
		filmTemp.setTitreO(item.getTitre());
		jmsTemplate.convertAndSend(topic, new JmsStatusMessage<Film>(JmsStatus.FILM_PROCESSOR_INIT, filmTemp,0l,JmsStatus.FILM_PROCESSOR_INIT.statusValue()));
		
		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("keycloak")
				.principal("fr/bluechipit/dvdtheque/batch")
				.build();

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientServiceAndManager.authorize(authorizeRequest);

		OAuth2AccessToken accessToken = Objects.requireNonNull(authorizedClient).getAccessToken();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", "Bearer " + accessToken.getTokenValue());
        HttpEntity<?> request = new HttpEntity<>(headers);
        
        
        /*
		ResponseEntity<Results> resultsResponse = restTemplate.exchange(environment.getRequiredProperty(TMDB_SERVICE_URL)
				+environment.getRequiredProperty(TMDB_SERVICE_BY_TMDBID)+"?tmdbId="+item.getTmdbId(), HttpMethod.GET, request, Results.class);
		
		if(resultsResponse != null && resultsResponse.getBody() != null) {
			request = new HttpEntity<>(resultsResponse.getBody(),headers);
			ResponseEntity<Film> response = restTemplate.exchange(environment.getRequiredProperty(DVDTHEQUE_SERVICE_URL)
					+environment.getRequiredProperty(DVDTHEQUE_SERVICE_TRANSFORM_BY_TMDBID)+"/"+item.getTmdbId(), 
					HttpMethod.PUT, 
					request, 
					Film.class);
			if(response != null && response.getBody() != null) {
				filmToSave = response.getBody();
			}
		}*/
		
        filmTemp.setOrigine(FilmOrigine.valueOf(item.getOrigine()));
		if(item.getOrigine().equalsIgnoreCase(FilmOrigine.DVD.name()) || item.getOrigine().equalsIgnoreCase(FilmOrigine.EN_SALLE.name())) {
			DvdBuilder dvdBuilder = new DvdBuilder();
			dvdBuilder.setFilmFormat(item.getFilmFormat());
			dvdBuilder.setFilmToSave(filmTemp);
			dvdBuilder.setZonedvd(item.getZonedvd());
			
			request = new HttpEntity<>(dvdBuilder,headers);
			ResponseEntity<Dvd> dvdResponse = restTemplate.exchange(environment.getRequiredProperty(DVDTHEQUE_SERVICE_URL)+environment.getRequiredProperty(DVDTHEQUE_SERVICE_BUILD_DVD),
					HttpMethod.POST, 
					request, 
					Dvd.class);
			filmTemp.setDvd(dvdResponse.getBody());
			boolean loadFromFile = Boolean.valueOf(environment.getRequiredProperty(RIPPEDFLAGTASKLET_FROM_FILE));
			if(!loadFromFile) {
				filmTemp.getDvd().setRipped(false);
			}else {
				if(StringUtils.isEmpty(item.getRipped())) {
					filmTemp.getDvd().setRipped(false);
				}else {
					filmTemp.getDvd().setRipped(item.getRipped().equalsIgnoreCase("oui")?true:false);
					if(item.getRipped().equalsIgnoreCase("oui") && StringUtils.isNotEmpty(item.getRipDate())) {
						DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
						filmTemp.getDvd().setDateRip(sdf.parse(item.getRipDate()));
					}
				}
			}
		}
		
		if(StringUtils.isEmpty(item.getVu())) {
			filmTemp.setVu(false);
		}else {
			filmTemp.setVu(item.getVu().equalsIgnoreCase("oui")?true:false);
		}
		if(StringUtils.isNotEmpty(item.getDateVue())) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			filmTemp.setDateVue(LocalDate.parse(item.getDateVue(),formatter));
		}else {
			filmTemp.setDateVue(null);
		}
		if(StringUtils.isNotEmpty(item.getDateSortieDvd())) {
			DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			filmTemp.setDateSortieDvd(sdf.parse(item.getDateSortieDvd()));
		}else {
			filmTemp.setDateSortieDvd(null);
		}
		if(StringUtils.isNotEmpty(item.getDateInsertion())) {
			DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			filmTemp.setDateInsertion(sdf.parse(item.getDateInsertion()));
		}else {
			Calendar cal = Calendar.getInstance(Locale.FRANCE);
			cal.set(Calendar.YEAR, 2018);
			cal.set(Calendar.MONTH, 7);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			filmTemp.setDateInsertion(cal.getTime());
		}
		
		//FicheFilm ficheFilm = restTemplate.getForObject(environment.getRequiredProperty(ALLOCINE_SERVICE_URL)+"?title="+item.getTitre(), FicheFilm.class);
		//filmToSave.setCritiquesPresse(ficheFilm.getCritiquesPresse());
		//allocineServiceClient.addCritiquesPresseToFilm(filmToSave);
		
		logger.debug(filmTemp.toString());
		watch.stop();
		jmsTemplate.convertAndSend(topic, new JmsStatusMessage<Film>(JmsStatus.FILM_PROCESSOR_COMPLETED, filmTemp,watch.getTime(),JmsStatus.FILM_PROCESSOR_COMPLETED.statusValue()));
		logger.debug("Film "+filmTemp.getTitre()+" processing Time Elapsed: " + watch.getTime());
		return filmTemp;
	}
}
