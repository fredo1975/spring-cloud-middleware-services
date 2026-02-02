package fr.bluechipit.dvdtheque.model;

import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.dao.domain.Film;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record FilmDto(Long id,
                      Integer annee,
                      LocalDate dateSortie,
                      LocalDate dateInsertion,
                      LocalDate dateSortieDvd,
                      String titre,
                      String titreO,
                      DvdDto dvd,
                      FilmOrigine origine,
                      boolean vu,
                      String posterPath,
                      Long tmdbId,
                      String overview,
                      Integer runtime,
                      String homepage,
                      boolean alreadyInDvdtheque,
                      LocalDateTime dateMaj,
                      LocalDate dateVue,
                      Integer allocineFicheFilmId,
                      Set<PersonneDto> realisateur,
                      Set<PersonneDto> acteur,
                      Set<GenreDto> genre,
                      List<CritiquePresseDto2> critiquePresse) {
    public static FilmDto of(Film film) {
        if (film == null) return null;
        return new FilmDto(film.getId(),
                film.getAnnee(),
                film.getDateSortie(),
                film.getDateInsertion(),
                film.getDateSortieDvd(),
                film.getTitre(),
                film.getTitreO(),
                DvdDto.of(film.getDvd()),
                film.getOrigine(),
                film.isVu(),
                film.getPosterPath(),
                film.getTmdbId(),
                film.getOverview(),
                film.getRuntime(),
                film.getHomepage(),
                film.isAlreadyInDvdtheque(),
                film.getDateMaj(),
                film.getDateVue(),
                film.getAllocineFicheFilmId(),
                PersonneDto.of(film.getRealisateur()),
                PersonneDto.of(film.getActeur()),
                GenreDto.of(film.getGenre()),
                CritiquePresseDto2.of(film.getCritiquePresse()));
    }
}
