package fr.bluechipit.dvdtheque.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private Long id;
    private Integer annee;
    private String titre;
    private String titreO;
    private Date dateSortie;
    private Date dateInsertion;
    private Date dateSortieDvd;
    private boolean vu;
    private String posterPath;
    private LocalDate dateVue;
    private Integer runtime;
    private String overview;
    private FilmOrigine origine;
    private PersonnesFilm personnesFilm;
}
