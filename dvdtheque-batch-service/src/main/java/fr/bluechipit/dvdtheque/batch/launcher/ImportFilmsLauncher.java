package fr.bluechipit.dvdtheque.batch.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
//@Component
public class ImportFilmsLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportFilmsLauncher.class);
	private final Job job;
    private final JobLauncher jobLauncher;
    
    //@Autowired
    ImportFilmsLauncher(@Qualifier("importFilmsJob") Job job, JobLauncher jobLauncher) {
        this.job = job;
        this.jobLauncher = jobLauncher;
    }

    void launchCsvFileToDatabaseJob() throws InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        LOGGER.info("Starting importFilms job");
        //jobLauncher.run(job, newExecution());
        LOGGER.info("Stopping importFilms job");
    }

    private JobParameters newExecution() {
        
        return new JobParameters();
    }
}
