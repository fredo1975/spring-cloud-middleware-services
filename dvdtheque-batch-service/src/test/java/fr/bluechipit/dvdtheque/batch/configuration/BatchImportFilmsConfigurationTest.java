package fr.bluechipit.dvdtheque.batch.configuration;

import enums.DvdFormat;
import model.Dvd;
import model.DvdBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;


@ActiveProfiles("test-import")
@SpringBootTest
@WithMockUser(roles = "user")
public class BatchImportFilmsConfigurationTest{
	//protected Logger logger = LoggerFactory.getLogger(BatchImportFilmsConfigurationTest.class);
	@Autowired
	@Qualifier(value = "importFilmsJob")
	private Job 			job;

	@Autowired
	private RestTemplate 	restTemplate;
	
	@Autowired
	private JobRepository 	jobRepository;

	@Value("${csv.dvd.file.name.import}")
    private String path;

    private String INPUT_FILE_PATH="INPUT_FILE_PATH";

	@MockBean
	AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager;

	@Test
	public void launchImportFilmsJob() throws Exception {
		Dvd dvd = new Dvd();
		dvd.setFormat(DvdFormat.DVD);
		dvd.setZone(2);
		dvd.setAnnee(2022);
		dvd.setRipped(false);

		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				ClientRegistration
						.withRegistrationId("myRemoteService")
						.clientId("dvdtheque")
						.clientSecret("sd")
						.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
						.tokenUri("sd")
						.build(),
				"some-keycloak",
				new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
						"c29tZS10b2tlbg==",
						Instant.now().minus(Duration.ofMinutes(1)),
						Instant.now().plus(Duration.ofMinutes(4))));

		Mockito.when(authorizedClientServiceAndManager.authorize(any())).thenReturn(authorizedClient);

		ResponseEntity<Dvd> dvdRes = new ResponseEntity<>(dvd, HttpStatus.ACCEPTED);
		Mockito.when(restTemplate.exchange(any(String.class), Mockito.<HttpMethod>any(), Mockito.<HttpEntity<DvdBuilder>>any(), Mockito.<Class<Dvd>>any()))
				.thenReturn(dvdRes);

		Calendar c = Calendar.getInstance();
		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addDate("TIMESTAMP", c.getTime());
		builder.addString(INPUT_FILE_PATH, path);
		JobParameters jobParameters = builder.toJobParameters();
		TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}
}
