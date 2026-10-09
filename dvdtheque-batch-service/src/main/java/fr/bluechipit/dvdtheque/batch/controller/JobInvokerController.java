package fr.bluechipit.dvdtheque.batch.controller;

import fr.bluechipit.dvdtheque.batch.film.backup.MultipartFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.security.RolesAllowed;
import java.io.File;

@RestController
@RequestMapping("/dvdtheque-batch-service/invokejob")
public class JobInvokerController {
	protected Logger logger = LoggerFactory.getLogger(JobInvokerController.class);
	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("runExportFilmsJob")
	Job runExportFilmsJob;
/*
	@Autowired
	@Qualifier("runExportCritiquesPresseJob")
	Job runExportCritiquesPresseJob;
*/
	@Autowired
	@Qualifier("importFilmsJob")
	Job importFilmsJob;

	@Autowired
	private MultipartFileUtil multipartFileUtil;

	@RolesAllowed("user")
	@RequestMapping("/exportFilmsJob")
	public String handleExportFilmsJob() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
				.toJobParameters();
		jobLauncher.run(runExportFilmsJob, jobParameters);
		return "Batch exportFilmsJob has been invoked";
	}
/*
	@RolesAllowed("user")
	@RequestMapping("/exportCritiquesPresseJob")
	public String handleCritiquesPresseJob() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
				.toJobParameters();
		jobLauncher.run(runExportCritiquesPresseJob, jobParameters);
		return "Batch exportCritiquesPresseJob has been invoked";
	}
*/
	@RolesAllowed("user")
	@PostMapping(value = "/importFilmsJob", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String handleImportFilmsJob(@RequestParam("file") MultipartFile file) throws Exception {

		File resFile;
		try {
			resFile = this.multipartFileUtil.createFileToImport(file);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw e;
		}

		// 3. Lancement du Job
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis())
				.addString("INPUT_FILE_PATH", resFile.getAbsolutePath())
				.toJobParameters();

		jobLauncher.run(importFilmsJob, jobParameters);

		return "Batch importFilmsJob has been invoked";
	}

	@RolesAllowed("user")
	@PostMapping(value = "/films/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<Void> importFilmList(@RequestParam("file") MultipartFile file) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, InvalidJobParametersException, JobRestartException {
		File resFile;
		try {
			resFile = this.multipartFileUtil.createFileToImport(file);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
				.addString("INPUT_FILE_PATH", resFile.getAbsolutePath()).toJobParameters();
		jobLauncher.run(importFilmsJob, jobParameters);
		return ResponseEntity.noContent().build();
	}
}
