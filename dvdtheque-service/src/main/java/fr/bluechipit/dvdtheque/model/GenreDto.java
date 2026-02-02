package fr.bluechipit.dvdtheque.model;

import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record GenreDto(Long id, String name, int tmdbId) {
    public static Set<GenreDto> of(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return Collections.emptySet();
        }
        return genres.stream().map(GenreDto::of).collect(Collectors.toSet());
    }
    public static GenreDto of(Genre genre) {
        if (genre == null) return null;
        return new GenreDto(genre.getId(), genre.getName(), genre.getTmdbId());
    }

}
