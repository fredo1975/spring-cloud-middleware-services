package fr.bluechipit.dvdtheque.allocine.controller;

import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.dto.FicheFilmRec;
import fr.bluechipit.dvdtheque.allocine.service.AllocineService;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/dvdtheque-allocine-service")
public class AllocineController {
	@Autowired
	private AllocineService allocineService;

	@RolesAllowed({"batch"})
	@GetMapping("/byTitle")
	public ResponseEntity<List<FicheFilmRec>> getAllocineFicheFilmByTitle(@RequestParam(name = "title", required = false) String title,
																		  @RequestParam(name = "titleO", required = false) String titleO) {
		List<FicheFilm> l = allocineService.retrieveFicheFilmByTitle(title);
		List<FicheFilmRec> ll = new ArrayList<>();
		if(CollectionUtils.isNotEmpty(l)) {
			for(FicheFilm ficheFilm : l) {
				ll.add(convertToDto(ficheFilm));
			}
		}else {
			l = allocineService.retrieveFicheFilmByTitle(titleO);
			if(CollectionUtils.isNotEmpty(l)) {
				for(FicheFilm ficheFilm : l) {
					ll.add(convertToDto(ficheFilm));
				}
			}
		}
		return ResponseEntity.ok(ll);
	}

	@RolesAllowed({"batch"})
	@GetMapping("/byId")
	public ResponseEntity<FicheFilmRec> getAllocineFicheFilmById(@RequestParam(name = "id", required = true) Integer id) {
		Optional<FicheFilm> ficheFilm = allocineService.retrieveFicheFilm(id);
        return ficheFilm.map(film -> ResponseEntity.ok(convertToDto(film))).orElseGet(() -> ResponseEntity.notFound().build());
    }
	private FicheFilmRec convertToDto(FicheFilm ficheFilm) {
		if(ficheFilm != null) {
            return FicheFilmRec.fromEntity(ficheFilm);
		}
		return null;
	}
	/*
	@GetMapping("/byFicheFilmId/{ficheFilmId}")
	public ResponseEntity<FicheFilm> getAllocineFicheFilmByFicheFilmId(@PathVariable("ficheFilmId")Integer ficheFilmId) {
		return ResponseEntity.ok(allocineService.retrievefindByFicheFilmId(ficheFilmId));
	}*/

	@PostMapping("/scraping-fichefilm")
	public ResponseEntity<Void> launchAllocineScrapingFicheFilm() {
		allocineService.scrapAllAllocineFicheFilmMultithreaded();
		return ResponseEntity.noContent().build();
	}
	
	@RolesAllowed("user")
	@GetMapping("/paginatedSarch")
	public ResponseEntity<Page<FicheFilmRec>> paginatedSarch(@RequestParam(name = "query", required = false)String query,
			@RequestParam(name = "offset", required = false)Integer offset,
			@RequestParam(name = "limit", required = false)Integer limit,
			@RequestParam(name = "sort", required = false)String sort){
		return ResponseEntity.ok(allocineService.paginatedSearch(query, offset, limit, sort));
	}
}
