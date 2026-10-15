package fr.bluechipit.dvdtheque.model;

import enums.DvdFormat;
import enums.FilmOrigine;
import fr.bluechipit.dvdtheque.dao.domain.Dvd;
import fr.bluechipit.dvdtheque.dao.domain.Film;
import fr.bluechipit.dvdtheque.dao.domain.Genre;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTOs et value objects du modèle - Unit Tests")
class ModelDtoTest {

    @Test
    @DisplayName("DvdDto.of() doit retourner null pour un dvd null")
    void dvdDto_ShouldReturnNullForNullDvd() {
        assertThat(DvdDto.of(null)).isNull();
    }

    @Test
    @DisplayName("DvdDto.of() doit mapper tous les champs")
    void dvdDto_ShouldMapAllFields() {
        Dvd dvd = new Dvd();
        dvd.setId(1L);
        dvd.setAnnee(2010);
        dvd.setZone(2);
        dvd.setEdition("Collector");
        dvd.setDateRip(LocalDate.of(2020, 1, 1));
        dvd.setFormat(DvdFormat.BLUERAY);
        dvd.setRipped(true);

        DvdDto dto = DvdDto.of(dvd);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.annee()).isEqualTo(2010);
        assertThat(dto.zone()).isEqualTo(2);
        assertThat(dto.edition()).isEqualTo("Collector");
        assertThat(dto.dateRip()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(dto.format()).isEqualTo(DvdFormat.BLUERAY);
        assertThat(dto.ripped()).isTrue();
    }

    @Test
    @DisplayName("GenreDto.of() doit gérer null, vide et éléments")
    void genreDto_ShouldHandleNullEmptyAndElements() {
        assertThat(GenreDto.of((Set<Genre>) null)).isEmpty();
        assertThat(GenreDto.of(Collections.emptySet())).isEmpty();
        assertThat(GenreDto.of((Genre) null)).isNull();

        Genre genre = new Genre(28, "Action");
        genre.setId(3L);
        assertThat(GenreDto.of(genre)).isEqualTo(new GenreDto(3L, "Action", 28));
        assertThat(GenreDto.of(Set.of(genre))).containsExactly(new GenreDto(3L, "Action", 28));
    }

    @Test
    @DisplayName("PersonneDto.of() doit convertir la date java.util en LocalDate")
    void personneDto_ShouldConvertDate() {
        Personne personne = new Personne();
        personne.setId(1L);
        personne.setNom("NOLAN");
        personne.setPrenom("Christopher");
        personne.setDateN(Date.from(LocalDate.of(1970, 7, 30).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        personne.setProfilePath("/p.jpg");

        PersonneDto dto = PersonneDto.of(personne);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nom()).isEqualTo("NOLAN");
        assertThat(dto.prenom()).isEqualTo("Christopher");
        assertThat(dto.dateN()).isEqualTo(LocalDate.of(1970, 7, 30));
        assertThat(dto.profilePath()).isEqualTo("/p.jpg");
    }

    @Test
    @DisplayName("PersonneDto.of() doit gérer date nulle, personne nulle et set vide")
    void personneDto_ShouldHandleNulls() {
        Personne personne = new Personne();
        personne.setNom("NOBIRTH");
        assertThat(PersonneDto.of(personne).dateN()).isNull();
        assertThat(PersonneDto.of((Personne) null)).isNull();
        assertThat(PersonneDto.of((Set<Personne>) null)).isEmpty();
        assertThat(PersonneDto.of(Collections.emptySet())).isEmpty();
    }

    @Test
    @DisplayName("CritiquePresseDto2.of() doit mapper une entité et une liste")
    void critiquePresseDto2_ShouldMapEntityAndList() {
        CritiquePresse cp = new CritiquePresse();
        cp.setNewsSource("Le Monde");
        cp.setRating(4.0);
        cp.setBody("Bien");
        cp.setAuthor("A. B.");

        CritiquePresseDto2 dto = CritiquePresseDto2.of(cp);
        assertThat(dto.newsSource()).isEqualTo("Le Monde");
        assertThat(dto.rating()).isEqualTo(4.0);

        assertThat(CritiquePresseDto2.of((CritiquePresse) null)).isNull();
        assertThat(CritiquePresseDto2.of((List<CritiquePresse>) null)).isNull();
        assertThat(CritiquePresseDto2.of(List.of(cp))).hasSize(1);
    }

    @Test
    @DisplayName("CritiquePresseDto2.of(liste) doit conserver les éléments nuls")
    void critiquePresseDto2_ShouldKeepNullElementsInList() {
        List<CritiquePresse> list = new ArrayList<>();
        list.add(null);

        List<CritiquePresseDto2> result = CritiquePresseDto2.of(list);

        assertThat(result).hasSize(1).containsNull();
    }

    @Test
    @DisplayName("CritiquePresse (modèle) doit être égal sur newsSource")
    void critiquePresse_ShouldBeEqualOnNewsSource() {
        CritiquePresse a = new CritiquePresse();
        a.setNewsSource("X");
        a.setRating(1.0);
        CritiquePresse b = new CritiquePresse();
        b.setNewsSource("X");
        b.setRating(5.0);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);

        CritiquePresse c = new CritiquePresse();
        c.setNewsSource("Y");
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("FicheFilmDto (modèle) doit être égal sur id et initialiser les critiques")
    void ficheFilmDto_ShouldBeEqualOnId() {
        FicheFilmDto a = new FicheFilmDto();
        a.setId(1);
        FicheFilmDto b = new FicheFilmDto();
        b.setId(1);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(new FicheFilmDto().getCritiquePresse()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("ZoneDvd.getById() doit résoudre les zones connues et renvoyer null sinon")
    void zoneDvd_ShouldResolveById() {
        assertThat(ZoneDvd.getById(1)).isEqualTo(ZoneDvd.Zone1);
        assertThat(ZoneDvd.getById(2)).isEqualTo(ZoneDvd.Zone2);
        assertThat(ZoneDvd.getById(3)).isNull();
        assertThat(ZoneDvd.Zone1.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("TypePersonne.getByType() doit être insensible à la casse et renvoyer null sinon")
    void typePersonne_ShouldResolveByType() {
        assertThat(TypePersonne.getByType("Realisateur")).isEqualTo(TypePersonne.REALISATEUR);
        assertThat(TypePersonne.getByType("acteur")).isEqualTo(TypePersonne.ACTEUR);
        assertThat(TypePersonne.getByType("unknown")).isNull();
        assertThat(TypePersonne.REALISATEUR.getType()).isEqualTo("Realisateur");
    }

    @Test
    @DisplayName("FilmListParamBuilder doit recopier tous les champs")
    void filmListParamBuilder_ShouldCopyAllFields() {
        Film film = new Film();
        List<Film> films = List.of(film);
        Personne realisateur = new Personne();
        Personne acteur = new Personne();
        Genre genre = new Genre(1, "Drama");

        FilmListParam param = new FilmListParamBuilder.Builder()
                .setFilms(films)
                .setRealisateurs(List.of(realisateur))
                .setActeurs(List.of(acteur))
                .setGenres(List.of(genre))
                .setRealisateursLength(1)
                .setActeursLength(1)
                .build();

        assertThat(param.getFilms()).isEqualTo(films);
        assertThat(param.getRealisateurs()).containsExactly(realisateur);
        assertThat(param.getActeurs()).containsExactly(acteur);
        assertThat(param.getGenres()).containsExactly(genre);
        assertThat(param.getRealisateursLength()).isEqualTo(1);
        assertThat(param.getActeursLength()).isEqualTo(1);
    }

    @Test
    @DisplayName("FilmDto.of() doit mapper un film complet (relations incluses)")
    void filmDto_ShouldMapFullFilm() {
        Film film = new Film();
        film.setId(1L);
        film.setTitre("INCEPTION");
        film.setOrigine(FilmOrigine.DVD);
        film.setVu(true);
        film.setAnnee(2010);
        film.setTmdbId(27205L);
        Dvd dvd = new Dvd();
        dvd.setId(9L);
        film.setDvd(dvd);
        Personne acteur = new Personne();
        acteur.setNom("DICAPRIO");
        film.setActeur(new HashSet<>(Set.of(acteur)));
        Genre genre = new Genre(28, "Action");
        film.setGenre(new HashSet<>(Set.of(genre)));
        CritiquePresse cp = new CritiquePresse();
        cp.setNewsSource("Le Monde");
        film.addCritiquePresse(cp);

        FilmDto dto = FilmDto.of(film);

        assertThat(dto.titre()).isEqualTo("INCEPTION");
        assertThat(dto.dvd()).isNotNull();
        assertThat(dto.dvd().id()).isEqualTo(9L);
        assertThat(dto.acteur()).hasSize(1);
        assertThat(dto.genre()).hasSize(1);
        assertThat(dto.critiquePresse()).hasSize(1);
    }
}
