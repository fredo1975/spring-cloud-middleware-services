package fr.bluechipit.dvdtheque.batch.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component(value="rippedFlagTasklet")
public class RippedFlagTasklet implements Tasklet{
	protected Logger logger = LoggerFactory.getLogger(RippedFlagTasklet.class);
	private static String LISTE_DVD_FILE_PATH="dvd.file.path";
	private static String RIPPEDFLAGTASKLET_FROM_FILE="rippedFlagTasklet.from.file";
	@Autowired
    Environment environment;
	
	/*@Value( "${file.extension}" )
	private String fileExtension;*/
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		/*
		boolean loadFromFile = Boolean.valueOf(environment.getRequiredProperty(RIPPEDFLAGTASKLET_FROM_FILE));
		if(!loadFromFile) {
			Resource directory = new FileSystemResource(environment.getRequiredProperty(LISTE_DVD_FILE_PATH));
			File dir = directory.getFile();
			Assert.notNull(directory, "directory must be set");
	        File[] files = dir.listFiles();
	        for (int i = 0; i < files.length; i++) {
	        	String name = files[i].getName();
	        	long millis = files[i].lastModified();
	        	Calendar cal = Calendar.getInstance(Locale.FRANCE);
	        	cal.setTimeInMillis(millis);
	        	String extension = StringUtils.substringAfter(name, ".");
	        	if(extension.equalsIgnoreCase("mkv")) {
	        		String titre = StringUtils.substringBefore(name, ".");
	        		try {
	        			Film film = filmService.findFilmByTitre(titre);
	        			if(film != null) {
	        				film.getDvd().setRipped(true);
	        				film.getDvd().setDateRip(cal.getTime());
	            			Film mergedFilm = filmService.updateFilm(film);
	            			logger.debug(mergedFilm.toString());
	        			}
	        		}catch(EmptyResultDataAccessException e) {
	        			//logger.error(titre+" not found");
	        		}
	        	}
	        }
		}else {
			logger.info("nothing to do");
		}*/
		return RepeatStatus.FINISHED;
	}
	
}
