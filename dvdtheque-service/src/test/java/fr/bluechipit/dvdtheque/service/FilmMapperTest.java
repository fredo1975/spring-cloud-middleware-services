package fr.bluechipit.dvdtheque.service;

import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.model.FilmDto;
import fr.bluechipit.dvdtheque.model.GenreDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FilmMapperTest {
    @Test
    @DisplayName("Should map full Film entity to DTO successfully")
    void shouldMapFullFilmToDto() {
        // GIVEN: A fully populated Film entity
        Film film = new Film();
        film.setId(1L);
        film.setTitre("Inception");
        Personne actuer = new Personne();
        actuer.setId(10L);
        actuer.setNom("DiCaprio");
        actuer.setPrenom("Leonardo");
        film.setActeur(Set.of(actuer));
        Genre genre = new Genre();
        genre.setId(1L);
        genre.setName("Sci-Fi");
        film.setGenre(Set.of(genre));

        // WHEN
        FilmDto dto = FilmDto.of(film);

        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.titre()).isEqualTo("Inception");
        assertThat(dto.acteur()).hasSize(1);
        assertThat(dto.acteur().iterator().next().nom()).isEqualTo("DiCaprio");
        assertThat(dto.genre()).extracting(GenreDto::name).containsExactly("Sci-Fi");
    }

    @Test
    @DisplayName("Should return empty collections instead of null when relations are missing")
    void shouldHandleNullRelationsGracefully() {
        // GIVEN: A film with null collections
        Film film = new Film();
        film.setTitre("Minimal Film");
        film.setActeur(null);
        film.setGenre(null);
        film.setDvd(null);

        // WHEN
        FilmDto dto = FilmDto.of(film);

        // THEN
        assertThat(dto.acteur()).isNotNull().isEmpty();
        assertThat(dto.genre()).isNotNull().isEmpty();
        assertThat(dto.dvd()).isNull(); // DVD is a single object, so null is appropriate here
    }

    @Test
    @DisplayName("Should return null when the input film is null")
    void shouldReturnNullWhenInputIsNull() {
        assertThat(FilmDto.of(null)).isNull();
    }
}
