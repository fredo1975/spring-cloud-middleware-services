package fr.bluechipit.dvdtheque.batch.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.bluechipit.dvdtheque.batch.writer.ExcelStreamFilmWriter;
import model.Film;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableScheduling
public class BatchExportFilmsConfiguration {
	protected Logger logger = LoggerFactory.getLogger(BatchExportFilmsConfiguration.class);
    @Autowired
    private JobRepository 											jobRepository;
    @Autowired
    private PlatformTransactionManager 								transactionManager;
    @Autowired
    private RestTemplate											restTemplate;
    @Autowired
    private Environment 											environment;
    @Autowired
    private AuthorizedClientServiceOAuth2AuthorizedClientManager 	authorizedClientServiceAndManager;
    public static String 											DVDTHEQUE_SERVICE_URL="dvdtheque-service.url";
	public static String 											DVDTHEQUE_SERVICE_ALL="dvdtheque-service.films";
	
	@Bean(name = "runExportFilmsJob")
	public Job runExportFilmsJob(Step exportFilmsStep) {
		return new JobBuilder("exportFilms", jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(exportFilmsStep  )
				.build();
	}
	@StepScope
    @Bean
    protected ListItemReader<Film> dvdthequeServiceFilmReader() {
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
        ResponseEntity<List<Film>> filmList = restTemplate.exchange(environment.getRequiredProperty(DVDTHEQUE_SERVICE_URL)+environment.getRequiredProperty(DVDTHEQUE_SERVICE_ALL)+"?displayType=TOUS", 
    			HttpMethod.GET, 
    			request, 
    			new ParameterizedTypeReference<List<Film>>(){});
        logger.info("Issued: filmList.getBody().size()=" + Objects.requireNonNull(filmList.getBody()).size());
        return new ListItemReader<>(filmList.getBody());
    }
    @Bean
    protected ExcelStreamFilmWriter excelFilmWriter() {
    	return new ExcelStreamFilmWriter();
    }

    @Bean
    protected Step exportFilmsStep(ListItemReader<Film> reader) { // Spring injectera le bean avec le bon scope
        return new StepBuilder("exportFilms", jobRepository)
                .<Film, Film>chunk(800, transactionManager)
                .reader(reader)
                .writer(excelFilmWriter())
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public ObjectMapper mapper() {
    	return new ObjectMapper();
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(15);
        taskExecutor.setMaxPoolSize(20);
        taskExecutor.setQueueCapacity(30);
        return taskExecutor;
    }
}
