package fr.bluechipit.dvdtheque.service;


import fr.bluechipit.dvdtheque.model.Film;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DVDthequeService {

    @Value("${dvdtheque-service.url}")
    private String dvdthequeServiceUrl;

    private final RestTemplate restTemplate;

    @Tool(description = "récupération des détails d'un film par titre de la dvdtheque")
    public Film getFilmByTitre(@ToolParam(description = "titre du film")String titre) {
        log.info("Récupération du FILM avec le titre: {}", titre);
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> request = new HttpEntity<>(null, headers);
            var film = restTemplate.exchange(dvdthequeServiceUrl +"/film/"+titre, HttpMethod.GET,request,Film.class);
            log.info("FILM récupéré: {}", film);
            return film.getBody();
        } catch (WebClientResponseException e) {
            log.error("Erreur lors de la récupération du FILM {}: {}", titre, e.getMessage());
            return null;
        }
    }
}
