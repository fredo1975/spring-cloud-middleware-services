package fr.bluechipit.dvdtheque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableDiscoveryClient
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class DvdthequeRestApplication {
	public static void main(String... args) {
		SpringApplication.run(DvdthequeRestApplication.class, args);
	}
}
