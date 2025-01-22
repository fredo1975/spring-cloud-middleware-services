package fr.bluechipit.dvdtheque.allocine.controller;

import fr.bluechipit.dvdtheque.allocine.service.AllocineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
	protected Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	@Autowired
	AllocineService allocineService;
	
	
	@Scheduled(cron = "${fichefilm.parsing.cron}")
	public void retrieveAllocineScrapingFicheFilm() {
		logger.info("retrieveAllocineScrapingFicheFilm start");
		allocineService.scrapAllAllocineFicheFilmMultithreaded();
		logger.info("retrieveAllocineScrapingFicheFilm end");
	}
	
}
