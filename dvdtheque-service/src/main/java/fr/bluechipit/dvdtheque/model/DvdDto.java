package fr.bluechipit.dvdtheque.model;

import enums.DvdFormat;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;

import java.time.LocalDate;

public record DvdDto(Long id,Integer annee,
                     Integer zone,
                     String edition,
                     LocalDate dateRip,
                     DvdFormat format,
                     boolean ripped) {
    public static DvdDto of(Dvd dvd){
        if (dvd == null) {
            return null; // Gracefully handles films with no DVD info
        }
        return new DvdDto(dvd.getId(),
                dvd.getAnnee(),
                dvd.getZone(),
                dvd.getEdition(),
                dvd.getDateRip(),
                dvd.getFormat(),
                dvd.isRipped());
    }
}
