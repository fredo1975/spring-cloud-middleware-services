package fr.bluechipit.dvdtheque.allocine.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import fr.bluechipit.dvdtheque.allocine.domain.CritiquePresse;
import fr.bluechipit.dvdtheque.allocine.domain.FicheFilm;
import fr.bluechipit.dvdtheque.allocine.dto.FicheFilmRec;
import fr.bluechipit.dvdtheque.allocine.repository.FicheFilmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import specifications.filter.SpecificationsBuilder;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AllocineServiceTest {
    @Mock
    private FicheFilmRepository ficheFilmRepository;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private Environment environment;

    @Mock
    private SpecificationsBuilder<FicheFilm> builder;

    @Mock
    private ExecutorService executorService;

    @Mock
    private IMap<Integer, FicheFilm> mapFicheFilms;

    @Mock
    private IMap<String, List<FicheFilm>> mapFicheFilmsByTitle;

    @Captor
    private ArgumentCaptor<List<FicheFilm>> ficheFilmListCaptor;

    @Captor
    private ArgumentCaptor<FicheFilm> ficheFilmCaptor;

    private AllocineService allocineService;

    @BeforeEach
    void setUp() {
        // Use doReturn for generic types to avoid type issues
        doReturn(mapFicheFilms).when(hazelcastInstance).getMap("ficheFilms");
        doReturn(mapFicheFilmsByTitle).when(hazelcastInstance).getMap("ficheFilmsByTitle");

        allocineService = new AllocineService(
                ficheFilmRepository,
                hazelcastInstance,
                environment,
                builder,
                executorService
        );

        // Set private fields
        ReflectionTestUtils.setField(allocineService, "nbParsedPage", 5);
        ReflectionTestUtils.setField(allocineService, "batchSize", 10);
        ReflectionTestUtils.setField(allocineService, "rateLimitMs", 100L);
    }

    // ==================== Paginated Search Tests ====================

    @Test
    void paginatedSearch_WithEmptyQuery_ShouldReturnAllFilms() {
        // Arrange
        List<FicheFilm> ficheFilms = createSampleFicheFilms(3);
        Page<FicheFilm> page = new PageImpl<>(ficheFilms);

        when(ficheFilmRepository.findAll(any(PageRequest.class))).thenReturn(page);

        // Act
        Page<FicheFilmRec> result = allocineService.paginatedSearch("", 1, 10, "-creationDate");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        verify(ficheFilmRepository).findAll(any(PageRequest.class));
        verify(ficheFilmRepository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void paginatedSearch_WithQuery_ShouldUseSpecification() {
        // Arrange
        String query = "title:Inception";
        List<FicheFilm> ficheFilms = createSampleFicheFilms(1);
        Page<FicheFilm> page = new PageImpl<>(ficheFilms);

        when(builder.with(query)).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Specification.class));
        when(ficheFilmRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        Page<FicheFilmRec> result = allocineService.paginatedSearch(query, 1, 10, "-creationDate");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(builder).with(query);
        verify(ficheFilmRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void paginatedSearch_WithNullParameters_ShouldUseDefaults() {
        // Arrange
        List<FicheFilm> ficheFilms = createSampleFicheFilms(2);
        Page<FicheFilm> page = new PageImpl<>(ficheFilms);

        when(ficheFilmRepository.findAll(any(PageRequest.class))).thenReturn(page);

        // Act
        Page<FicheFilmRec> result = allocineService.paginatedSearch(null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        verify(ficheFilmRepository).findAll(any(PageRequest.class));
    }

    // ==================== Retrieve Methods Tests ====================

    @Test
    void retrieveAllFicheFilm_ShouldReturnAllFilms() {
        // Arrange
        List<FicheFilm> ficheFilms = createSampleFicheFilms(5);
        when(ficheFilmRepository.findAll()).thenReturn(ficheFilms);

        // Act
        List<FicheFilm> result = allocineService.retrieveAllFicheFilm();

        // Assert
        assertThat(result).hasSize(5);
        verify(ficheFilmRepository).findAll();
    }

    @Test
    void retrieveFicheFilm_WithValidId_ShouldReturnFilm() {
        // Arrange
        int filmId = 1;
        FicheFilm ficheFilm = createFicheFilm(filmId, "Test Film");
        when(ficheFilmRepository.findById(filmId)).thenReturn(Optional.of(ficheFilm));

        // Act
        Optional<FicheFilm> result = allocineService.retrieveFicheFilm(filmId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAllocineFilmId()).isEqualTo(filmId);
        verify(ficheFilmRepository).findById(filmId);
    }

    @Test
    void retrieveFicheFilm_WithInvalidId_ShouldReturnEmpty() {
        // Arrange
        int filmId = 999;
        when(ficheFilmRepository.findById(filmId)).thenReturn(Optional.empty());

        // Act
        Optional<FicheFilm> result = allocineService.retrieveFicheFilm(filmId);

        // Assert
        assertThat(result).isEmpty();
        verify(ficheFilmRepository).findById(filmId);
    }

    // ==================== Retrieve By Title Tests ====================

    @Test
    void retrieveFicheFilmByTitle_WithCachedTitle_ShouldReturnFromCache() {
        // Arrange
        String title = "Inception";
        List<FicheFilm> cachedFilms = createSampleFicheFilms(2);
        when(mapFicheFilmsByTitle.get("INCEPTION")).thenReturn(cachedFilms);

        // Act
        List<FicheFilm> result = allocineService.retrieveFicheFilmByTitle(title);

        // Assert
        assertThat(result).hasSize(2);
        verify(mapFicheFilmsByTitle).get("INCEPTION");
        verify(ficheFilmRepository, never()).findByTitle(anyString());
    }

    @Test
    void retrieveFicheFilmByTitle_WithUncachedTitle_ShouldQueryDatabase() {
        // Arrange
        String title = "The Matrix";
        List<FicheFilm> dbFilms = createSampleFicheFilms(1);
        dbFilms.get(0).setTitle(title);

        when(mapFicheFilmsByTitle.get("THE MATRIX")).thenReturn(null);
        when(ficheFilmRepository.findByTitle(title)).thenReturn(dbFilms);

        // Act
        List<FicheFilm> result = allocineService.retrieveFicheFilmByTitle(title);

        // Assert
        assertThat(result).hasSize(1);
        verify(ficheFilmRepository).findByTitle(title);
        verify(mapFicheFilmsByTitle).putIfAbsent("THE MATRIX", result);
    }

    @Test
    void retrieveFicheFilmByTitle_WithEmptyTitle_ShouldReturnEmptyList() {
        // Act
        List<FicheFilm> result = allocineService.retrieveFicheFilmByTitle("");

        // Assert
        assertThat(result).isEmpty();
        verify(ficheFilmRepository, never()).findByTitle(anyString());
    }

    @Test
    void retrieveFicheFilmByTitle_WithNullTitle_ShouldReturnEmptyList() {
        // Act
        List<FicheFilm> result = allocineService.retrieveFicheFilmByTitle(null);

        // Assert
        assertThat(result).isEmpty();
        verify(ficheFilmRepository, never()).findByTitle(anyString());
    }

    // ==================== Find By FicheFilm ID Tests ====================

    @Test
    void findByFicheFilmId_WithCachedId_ShouldReturnFromCache() {
        // Arrange
        Integer filmId = 123;
        FicheFilm cachedFilm = createFicheFilm(filmId, "Cached Film");
        when(mapFicheFilms.get(filmId)).thenReturn(cachedFilm);

        // Act
        Optional<FicheFilm> result = allocineService.findByFicheFilmId(filmId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAllocineFilmId()).isEqualTo(filmId);
        verify(mapFicheFilms).get(filmId);
        verify(ficheFilmRepository, never()).findByFicheFilmId(anyInt());
    }

    @Test
    void findByFicheFilmId_WithUncachedId_ShouldQueryDatabase() {
        // Arrange
        Integer filmId = 456;
        FicheFilm dbFilm = createFicheFilm(filmId, "Database Film");

        when(mapFicheFilms.get(filmId)).thenReturn(null);
        when(ficheFilmRepository.findByFicheFilmId(filmId)).thenReturn(dbFilm);

        // Act
        Optional<FicheFilm> result = allocineService.findByFicheFilmId(filmId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAllocineFilmId()).isEqualTo(filmId);
        verify(ficheFilmRepository).findByFicheFilmId(filmId);
        verify(mapFicheFilms).putIfAbsent(filmId, dbFilm);
    }

    @Test
    void findByFicheFilmId_WithNullId_ShouldReturnEmpty() {
        // Act
        Optional<FicheFilm> result = allocineService.findByFicheFilmId(null);

        // Assert
        assertThat(result).isEmpty();
        verify(ficheFilmRepository, never()).findByFicheFilmId(anyInt());
    }

    @Test
    void findByFicheFilmId_WithNonExistentId_ShouldReturnEmpty() {
        // Arrange
        Integer filmId = 999;
        when(mapFicheFilms.get(filmId)).thenReturn(null);
        when(ficheFilmRepository.findByFicheFilmId(filmId)).thenReturn(null);

        // Act
        Optional<FicheFilm> result = allocineService.findByFicheFilmId(filmId);

        // Assert
        assertThat(result).isEmpty();
        verify(ficheFilmRepository).findByFicheFilmId(filmId);
    }

    // ==================== Save Methods Tests ====================

    @Test
    void saveFicheFilm_WithValidFilm_ShouldSaveSuccessfully() {
        // Arrange
        FicheFilm ficheFilm = createFicheFilm(1, "Test Film");
        when(ficheFilmRepository.save(ficheFilm)).thenReturn(ficheFilm);

        // Act
        FicheFilm result = allocineService.saveFicheFilm(ficheFilm);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAllocineFilmId()).isEqualTo(1);
        verify(ficheFilmRepository).save(ficheFilm);
    }

    @Test
    void saveFicheFilm_WithException_ShouldReturnNull() {
        // Arrange
        FicheFilm ficheFilm = createFicheFilm(1, "Test Film");
        when(ficheFilmRepository.save(ficheFilm))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        FicheFilm result = allocineService.saveFicheFilm(ficheFilm);

        // Assert
        assertThat(result).isNull();
        verify(ficheFilmRepository).save(ficheFilm);
    }

    @Test
    void saveFicheFilmBatch_WithValidList_ShouldSaveAll() {
        // Arrange
        List<FicheFilm> ficheFilms = createSampleFicheFilms(5);

        // Act
        allocineService.saveFicheFilmBatch(ficheFilms);

        // Assert
        verify(ficheFilmRepository).saveAll(ficheFilms);
    }

    @Test
    void saveFicheFilmBatch_WithEmptyList_ShouldNotCallRepository() {
        // Act
        allocineService.saveFicheFilmBatch(Collections.emptyList());

        // Assert
        verify(ficheFilmRepository, never()).saveAll(anyList());
    }

    @Test
    void saveFicheFilmBatch_WithNullList_ShouldNotCallRepository() {
        // Act
        allocineService.saveFicheFilmBatch(null);

        // Assert
        verify(ficheFilmRepository, never()).saveAll(anyList());
    }

    @Test
    void saveFicheFilmBatch_WithException_ShouldLogError() {
        // Arrange
        List<FicheFilm> ficheFilms = createSampleFicheFilms(3);
        doThrow(new RuntimeException("Database error"))
                .when(ficheFilmRepository).saveAll(anyList());

        // Act & Assert - should not throw exception
        assertThatCode(() -> allocineService.saveFicheFilmBatch(ficheFilms))
                .doesNotThrowAnyException();

        verify(ficheFilmRepository).saveAll(ficheFilms);
    }

    // ==================== Cache Tests ====================

    @Test
    void findInCacheByFicheFilmId_WithCachedId_ShouldReturnFilm() {
        // Arrange
        Integer filmId = 100;
        FicheFilm cachedFilm = createFicheFilm(filmId, "Cached");
        when(mapFicheFilms.get(filmId)).thenReturn(cachedFilm);

        // Act
        Optional<FicheFilm> result = ReflectionTestUtils.invokeMethod(
                allocineService, "findInCacheByFicheFilmId", filmId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAllocineFilmId()).isEqualTo(filmId);
    }

    @Test
    void findInCacheByFicheFilmTitle_WithCachedTitle_ShouldReturnFilms() {
        // Arrange
        String title = "CACHED FILM";
        List<FicheFilm> cachedFilms = createSampleFicheFilms(2);
        when(mapFicheFilmsByTitle.get(title)).thenReturn(cachedFilms);

        // Act
        Optional<List<FicheFilm>> result = ReflectionTestUtils.invokeMethod(
                allocineService, "findInCacheByFicheFilmTitle", title);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
    }

    // ==================== Integration Tests ====================

    @Test
    void scrapAllAllocineFicheFilmMultithreaded_ShouldSubmitCorrectNumberOfTasks() {
        // Arrange
        ReflectionTestUtils.setField(allocineService, "nbParsedPage", 3);

        @SuppressWarnings("unchecked")
        Future<AllocineService.PageResult> mockFuture = mock(Future.class);
        when(executorService.submit(any(Callable.class))).thenReturn(mockFuture);

        try {
            AllocineService.PageResult pageResult = new AllocineService.PageResult(1, 10, true, null);
            when(mockFuture.get()).thenReturn(pageResult);

            // Act
            AllocineService.ScrapingResult result = allocineService.scrapAllAllocineFicheFilmMultithreaded();

            // Assert
            verify(executorService, times(3)).submit(any(Callable.class));
            assertThat(result).isNotNull();
            assertThat(result.getSuccessfulPages()).isEqualTo(3);
            assertThat(result.getFailedPages()).isEqualTo(0);

        } catch (Exception e) {
            fail("Should not throw exception", e);
        }
    }

    // ==================== Helper Methods ====================

    private List<FicheFilm> createSampleFicheFilms(int count) {
        List<FicheFilm> films = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            films.add(createFicheFilm(i, "Film " + i));
        }
        return films;
    }

    private FicheFilm createFicheFilm(int id, String title) {
        FicheFilm ficheFilm = new FicheFilm();
        ficheFilm.setId((int) id);
        ficheFilm.setAllocineFilmId(id);
        ficheFilm.setTitle(title);
        ficheFilm.setUrl("https://www.allocine.fr/film/fichefilm-" + id + "/critiques/presse/");
        ficheFilm.setPageNumber(1);
        ficheFilm.setCritiquePresse(new HashSet<>());
        return ficheFilm;
    }

    private FicheFilm createFicheFilmWithCritiques(int id, String title, int critiqueCount) {
        FicheFilm ficheFilm = createFicheFilm(id, title);

        for (int i = 0; i < critiqueCount; i++) {
            CritiquePresse critique = new CritiquePresse();
            critique.setNewsSource("Source " + i);
            critique.setAuthor("Author " + i);
            critique.setBody("Review body " + i);
            critique.setRating((double) (i % 5 + 1));
            critique.setFicheFilm(ficheFilm);
            ficheFilm.addCritiquePresse(critique);
        }

        return ficheFilm;
    }

    // ==================== Inner Class Tests ====================

    @Test
    void pageResult_ShouldStoreCorrectValues() {
        // Act
        AllocineService.PageResult result = new AllocineService.PageResult(5, 10, true, null);

        // Assert
        assertThat(result.getPageNumber()).isEqualTo(5);
        assertThat(result.getSavedCount()).isEqualTo(10);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void pageResult_WithError_ShouldStoreErrorMessage() {
        // Act
        AllocineService.PageResult result = new AllocineService.PageResult(
                3, 0, false, "Connection timeout");

        // Assert
        assertThat(result.getPageNumber()).isEqualTo(3);
        assertThat(result.getSavedCount()).isEqualTo(0);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Connection timeout");
    }

    @Test
    void scrapingResult_ShouldCalculateCorrectTotals() {
        // Act
        AllocineService.ScrapingResult result = new AllocineService.ScrapingResult(10, 2, 150);

        // Assert
        assertThat(result.getSuccessfulPages()).isEqualTo(10);
        assertThat(result.getFailedPages()).isEqualTo(2);
        assertThat(result.getTotalSaved()).isEqualTo(150);
    }

    @Test
    void scrapingResult_ToString_ShouldFormatCorrectly() {
        // Arrange
        AllocineService.ScrapingResult result = new AllocineService.ScrapingResult(8, 1, 95);

        // Act
        String resultString = result.toString();

        // Assert
        assertThat(resultString).contains("successful=8");
        assertThat(resultString).contains("failed=1");
        assertThat(resultString).contains("totalSaved=95");
    }
}
