package fr.bluechipit.dvdtheque.allocine.dto;

import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record FicheFilmRec(int id, int allocineFilmId, String url, int pageNumber, String title,
                           Set<CritiquePresseRec> critiquePresse, LocalDateTime creationDate) {
    public static FicheFilmRec fromEntity(FicheFilm ficheFilm) {
        if (ficheFilm == null) return null;
        Set<CritiquePresse> critiques = ficheFilm.getCritiquePresse();
        Set<CritiquePresseRec> critiquesRec = critiques.stream().map(CritiquePresseRec::fromEntity).collect(Collectors.toSet());
        return new FicheFilmRec(
                ficheFilm.getId(),
                ficheFilm.getAllocineFilmId(),
                ficheFilm.getUrl(),
                ficheFilm.getPageNumber(),
                ficheFilm.getTitle(),
                critiquesRec,
                ficheFilm.getCreationDate());
    }
}
