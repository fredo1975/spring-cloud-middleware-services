package fr.bluechipit.dvdtheque.allocine.config.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class TaskSchedulerConfiguration {
	@Bean
	public TaskScheduler  taskScheduler() {
	    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
	    threadPoolTaskScheduler.setPoolSize(5);
	    threadPoolTaskScheduler.setThreadNamePrefix("DvdthequeThreadPoolTaskScheduler");
	    return threadPoolTaskScheduler;
	}
	
	@Bean("fixedThreadPool")
	public ExecutorService fixedThreadPool() {
		return Executors.newFixedThreadPool(5);
	}
}
