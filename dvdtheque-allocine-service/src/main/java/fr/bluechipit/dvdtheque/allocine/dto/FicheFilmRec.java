package fr.bluechipit.dvdtheque.allocine.dto;

import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import java.time.LocalDateTime;
import java.util.Set;

public record FicheFilmRec(int id, int allocineFilmId, String url, int pageNumber, String title,
                           Set<CritiquePresse> critiquePresse, LocalDateTime creationDate) {
    public static FicheFilmRec fromEntity(FicheFilm ficheFilm) {
        if (ficheFilm == null) return null;
        return new FicheFilmRec(
                ficheFilm.getId(),
                ficheFilm.getAllocineFilmId(),
                ficheFilm.getUrl(),
                ficheFilm.getPageNumber(),
                ficheFilm.getTitle(),
                ficheFilm.getCritiquePresse(),
                ficheFilm.getCreationDate());
    }
}
