package fr.bluechipit.dvdtheque.batch.configuration;

import enums.DvdFormat;
import enums.FilmOrigine;
import jakarta.jms.Topic;
import model.Dvd;
import model.Film;
import model.Personne;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ActiveProfiles("test-export")
@SpringBootTest
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    StepScopeTestExecutionListener.class})
	@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class BatchExportFilmsConfigurationTest {
	protected Logger logger = LoggerFactory.getLogger(BatchExportFilmsConfigurationTest.class);
	@Autowired
	@Qualifier(value = "runExportFilmsJob")
	private Job						job;
	
	@Autowired
	private RestTemplate 			restTemplate;
	
	@Autowired
	private JobRepository 			jobRepository;
	
	private Film buildfilm() {
		Film film = new Film();
		film.setAnnee(2012);
		film.setId(1l);
		film.setDvd(new Dvd());
		film.getDvd().setAnnee(2013);
		film.getDvd().setDateRip(Date.from(LocalDate.of(2013, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		film.getDvd().setFormat(DvdFormat.DVD);
		film.getDvd().setRipped(true);
		film.getDvd().setZone(2);
		film.setDateInsertion(Date.from(LocalDate.of(2013, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		film.setActeur(new HashSet<Personne>());
		Personne act1 = new Personne();
		act1.setId(1l);
		act1.setNom("Tom Cruise");
		film.getActeur().add(act1);
		Personne real = new Personne();
		real.setId(2l);
		real.setNom("David Lynch");
		film.setRealisateur(new HashSet<Personne>());
		film.getRealisateur().add(real);
		film.setTitre("film");
		film.setOrigine(FilmOrigine.DVD);
		film.setTmdbId(1l);
		film.setVu(false);
		return film;
	}
	@SuppressWarnings("unchecked")
	@Test
	public void launchExportFilmsJob() throws Exception {
		List<Film> l = new ArrayList<>();
		l.add(buildfilm());
        ResponseEntity<List<Film>> filmList = new ResponseEntity<List<Film>>(l,HttpStatus.ACCEPTED);
        Mockito.when(restTemplate.exchange(Mockito.any(String.class),
        		Mockito.<HttpMethod> any(),
                Mockito.<HttpEntity<?>> any(),
    			Mockito.any(ParameterizedTypeReference.class)))
        .thenReturn(filmList);
		Calendar c = Calendar.getInstance();
		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addDate("TIMESTAMP", c.getTime());
		JobParameters jobParameters = builder.toJobParameters();
		TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}
	
	@Bean
	JmsTemplate jmsTemplate() {
		return Mockito.mock(JmsTemplate.class);
	}

	@Bean
	Topic topic() {
		return Mockito.mock(Topic.class);
	}

	@Bean
	ClientRegistration dvdthequeClientRegistration(
			@Value("${spring.security.oauth2.client.provider.keycloak.token-uri}") String token_uri,
			@Value("${spring.security.oauth2.client.registration.keycloak.client-secret}") String client_secret,
			@Value("${spring.security.oauth2.client.registration.keycloak.authorization-grant-type}") String authorizationGrantType) {
		return ClientRegistration.withRegistrationId("keycloak").tokenUri(token_uri).clientId("dvdtheque-api")
				.clientSecret(client_secret).authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
				.build();
	}

	@Bean
	AuthorizedClientServiceOAuth2AuthorizedClientManager clientManager(ClientRegistration dvdthequeClientRegistration) {
		ClientRegistrationRepository clientRegistrationRepository = new InMemoryClientRegistrationRepository(
				dvdthequeClientRegistration);
		Map<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> authorizedClients = new HashMap<>();
		authorizedClients.put(new OAuth2AuthorizedClientId("keycloak", "batch"),
				new OAuth2AuthorizedClient(dvdthequeClientRegistration, "batch",
						new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "cscs", Instant.now(), Instant.MAX)));
		OAuth2AuthorizedClientService authorizedClientService = new InMemoryOAuth2AuthorizedClientService(
				clientRegistrationRepository, authorizedClients);
		return new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
				authorizedClientService);
	}
	
	@Bean(name = "dataSource")
	public DataSource dataSource() {
	    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
	    return builder.setType(EmbeddedDatabaseType.HSQL)
	      .addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
	      .addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
	      .build();
	}
}
