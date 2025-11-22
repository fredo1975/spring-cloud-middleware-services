package fr.bluechipit.dvdtheque.allocine.dto;

import java.util.Set;

public record FicheFilmRec(int id, int allocineFilmId, String url, int pageNumber, String title,
                           Set<CritiquePresse> critiquePresse, String creationDate) {}
