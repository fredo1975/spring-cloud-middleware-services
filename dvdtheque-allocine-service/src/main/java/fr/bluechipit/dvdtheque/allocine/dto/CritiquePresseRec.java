package fr.bluechipit.dvdtheque.allocine.dto;

import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;

public record CritiquePresseRec(int id, String newsSource, Double rating, String body, String author) {
    public static CritiquePresseRec fromEntity(CritiquePresse critiquePresse) {
        if (critiquePresse == null) return null;
        return new CritiquePresseRec(
                critiquePresse.getId(),
                critiquePresse.getNewsSource(),
                critiquePresse.getRating(),
                critiquePresse.getBody(),
                critiquePresse.getAuthor());
    }
}
