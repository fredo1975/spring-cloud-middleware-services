package fr.bluechipit.dvdtheque.model;

import fr.bluechipit.dvdtheque.dao.domain.Personne;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record PersonneDto(Long id, String nom, String prenom, LocalDate dateN, String profilePath) {

    public static  Set<PersonneDto> of(Set<Personne> personnes) {
        if (personnes == null || personnes.isEmpty()) {
            return Collections.emptySet();
        }
        return personnes.stream().map(PersonneDto::of).collect(Collectors.toSet());
    }

    public static PersonneDto of(Personne personne) {
        if (personne == null) {
            return null;
        }
        return new PersonneDto(personne.getId(), personne.getNom(), personne.getPrenom(),
                personne.getDateN() != null ? LocalDate.ofInstant(personne.getDateN().toInstant(), java.time.ZoneId.systemDefault()) : null,
                personne.getProfilePath());
    }
}
