package fr.bluechipit.dvdtheque.model;

import lombok.Data;

import java.util.Set;
@Data
public class PersonnesFilm {
    private Realisateur realisateur;
    private Set<Acteur> acteurs;
}
