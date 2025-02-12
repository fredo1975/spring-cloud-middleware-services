package fr.bluechipit.dvdtheque.batch.controller;

import fr.bluechipit.dvdtheque.batch.film.backup.ExcelFilmHandler;
import fr.bluechipit.dvdtheque.batch.film.backup.MultipartFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/dvdtheque-batch-service/invokejob")
public class JobInvokerController {
	protected Logger logger = LoggerFactory.getLogger(JobInvokerController.class);
	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("runExportFilmsJob")
	Job runExportFilmsJob;

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

	@RolesAllowed("user")
	@RequestMapping("/importFilmsJob")
	@PostMapping("/importFilmsJob")
	public String handleImportFilmsJob(@RequestBody byte[] csvBytes) throws Exception {

		try (FileOutputStream fos = new FileOutputStream("/tmp/films.xlsx")) {
			fos.write(csvBytes);
			//fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream

		}
		FileSystemResource resource = new FileSystemResource("/tmp/films.xlsx");
		if (!resource.exists()) {
			throw new IllegalStateException("filePath must exist : " + resource.getPath());
		}
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
				.addString("INPUT_FILE_PATH", resource.getPath()).toJobParameters();
		jobLauncher.run(importFilmsJob, jobParameters);
		return "Batch importFilmsJob has been invoked";

	}

	@RolesAllowed("user")
	@PostMapping("/films/import")
	ResponseEntity<Void> importFilmList(@RequestParam("file") MultipartFile file) throws IOException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
		File resFile = null;
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
