package fr.bluechipit.dvdtheque.allocine;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
//@ComponentScan(basePackages = {"specifications.filter","fr.bluechipit.dvdtheque.allocine.controller"})
public class AllocineServiceApplication {
	public static void main(String[] args){
        SpringApplication.run(AllocineServiceApplication.class,args);
    }
	
	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}
}
