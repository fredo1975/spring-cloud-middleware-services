package fr.bluechipit.dvdtheque.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import fr.bluechipit.dvdtheque.dao.domain.Personne;
import fr.bluechipit.dvdtheque.dao.repository.PersonneDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonneService Unit Tests")
public class PersonneServiceTest {
    @Mock
    private PersonneDao personneDao;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private IMap<Long, Personne> mapPersonnes;

    private PersonneService personneService;

    private Personne testPersonne1;
    private Personne testPersonne2;
    private Personne testPersonne3;

    @BeforeEach
    void setUp() {
        // Configuration du mock Hazelcast
        when(hazelcastInstance.<Long, Personne>getMap(PersonneService.CACHE_PERSONNE))
                .thenReturn(mapPersonnes);

        // Initialisation du service
        personneService = new PersonneService(personneDao, hazelcastInstance);

        // Données de test
        setupTestData();
    }

    private void setupTestData() {
        testPersonne1 = new Personne();
        testPersonne1.setId(1L);
        testPersonne1.setNom("LEONARDO DICAPRIO");
        testPersonne1.setProfilePath("/profile1.jpg");

        testPersonne2 = new Personne();
        testPersonne2.setId(2L);
        testPersonne2.setNom("CHRISTOPHER NOLAN");
        testPersonne2.setProfilePath("/profile2.jpg");

        testPersonne3 = new Personne();
        testPersonne3.setId(3L);
        testPersonne3.setNom("BRAD PITT");
        testPersonne3.setProfilePath("/profile3.jpg");
    }

    // ==================== Tests d'initialisation ====================

    @Test
    @DisplayName("init() doit récupérer la map du cache Hazelcast")
    void init_ShouldGetMapFromHazelcast() {
        // Given/When - déjà fait dans setUp()

        // Then
        verify(hazelcastInstance).getMap(PersonneService.CACHE_PERSONNE);
    }

    @Test
    void service_ShouldInitializeCorrectly() {
        // Given - Nouveaux mocks pour ce test
        HazelcastInstance newHazelcastInstance = mock(HazelcastInstance.class);
        IMap<Long, Personne> newMapPersonnes = mock(IMap.class);
        when(newHazelcastInstance.<Long, Personne>getMap(PersonneService.CACHE_PERSONNE))
                .thenReturn(newMapPersonnes);

        // When - Création avec les nouveaux mocks
        PersonneService newService = new PersonneService(personneDao, newHazelcastInstance);

        // Then - Vérification sur le nouveau mock
        assertThat(newService).isNotNull();
        verify(newHazelcastInstance).getMap(PersonneService.CACHE_PERSONNE);
    }

    // ==================== Tests de récupération ====================

    @Nested
    @DisplayName("Tests de findByPersonneId()")
    class FindByPersonneIdTests {

        @Test
        @DisplayName("doit retourner la personne depuis le cache si elle existe")
        void findByPersonneId_ShouldReturnFromCacheIfExists() {
            // Given
            when(mapPersonnes.get(1L)).thenReturn(testPersonne1);

            // When
            Personne result = personneService.findByPersonneId(1L);

            // Then
            assertThat(result).isEqualTo(testPersonne1);
            verify(mapPersonnes).get(1L);
            verify(personneDao, never()).findById(anyLong());
        }

        @Test
        @DisplayName("doit retourner la personne depuis la DB si absente du cache")
        void findByPersonneId_ShouldReturnFromDatabaseIfNotInCache() {
            // Given
            when(mapPersonnes.get(1L)).thenReturn(null);
            when(personneDao.findById(1L)).thenReturn(Optional.of(testPersonne1));

            // When
            Personne result = personneService.findByPersonneId(1L);

            // Then
            assertThat(result).isEqualTo(testPersonne1);
            verify(mapPersonnes).get(1L);
            verify(personneDao).findById(1L);
            verify(mapPersonnes).put(1L, testPersonne1);
        }

        @Test
        @DisplayName("doit retourner null si la personne n'existe ni en cache ni en DB")
        void findByPersonneId_ShouldReturnNullIfNotExists() {
            // Given
            when(mapPersonnes.get(99L)).thenReturn(null);
            when(personneDao.findById(99L)).thenReturn(Optional.empty());

            // When
            Personne result = personneService.findByPersonneId(99L);

            // Then
            assertThat(result).isNull();
            verify(mapPersonnes).get(99L);
            verify(personneDao).findById(99L);
            verify(mapPersonnes, never()).put(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Tests de getPersonne()")
    class GetPersonneTests {

        @Test
        @DisplayName("doit retourner la personne depuis le cache")
        void getPersonne_ShouldReturnFromCache() {
            // Given
            when(mapPersonnes.get(1L)).thenReturn(testPersonne1);

            // When
            Personne result = personneService.getPersonne(1L);

            // Then
            assertThat(result).isEqualTo(testPersonne1);
            verify(mapPersonnes).get(1L);
        }

        @Test
        @DisplayName("doit charger depuis la DB et mettre en cache")
        void getPersonne_ShouldLoadFromDatabaseAndCache() {
            // Given
            when(mapPersonnes.get(1L)).thenReturn(null);
            when(personneDao.findById(1L)).thenReturn(Optional.of(testPersonne1));

            // When
            Personne result = personneService.getPersonne(1L);

            // Then
            assertThat(result).isEqualTo(testPersonne1);
            verify(mapPersonnes).put(1L, testPersonne1);
        }
    }

    @Nested
    @DisplayName("Tests de loadPersonne()")
    class LoadPersonneTests {

        @Test
        @DisplayName("doit fonctionner identiquement à getPersonne()")
        void loadPersonne_ShouldWorkLikeGetPersonne() {
            // Given
            when(mapPersonnes.get(1L)).thenReturn(testPersonne1);

            // When
            Personne result = personneService.loadPersonne(1L);

            // Then
            assertThat(result).isEqualTo(testPersonne1);
        }
    }

    // ==================== Tests de findAllPersonne() ====================

    @Test
    @DisplayName("findAllPersonne() doit retourner les personnes du cache triées par nom")
    void findAllPersonne_ShouldReturnSortedPersonnesFromCache() {
        // Given
        Collection<Personne> cachedPersonnes = Arrays.asList(
                testPersonne1, // LEONARDO DICAPRIO
                testPersonne2, // CHRISTOPHER NOLAN
                testPersonne3  // BRAD PITT
        );
        when(mapPersonnes.values()).thenReturn(cachedPersonnes);

        // When
        List<Personne> result = personneService.findAllPersonne();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getNom()).isEqualTo("BRAD PITT");
        assertThat(result.get(1).getNom()).isEqualTo("CHRISTOPHER NOLAN");
        assertThat(result.get(2).getNom()).isEqualTo("LEONARDO DICAPRIO");
        verify(personneDao, never()).findAll();
    }

    @Test
    @DisplayName("findAllPersonne() doit charger depuis la DB si cache vide")
    void findAllPersonne_ShouldLoadFromDatabaseIfCacheEmpty() {
        // Given
        when(mapPersonnes.values()).thenReturn(Collections.emptyList());
        when(personneDao.findAll()).thenReturn(Arrays.asList(testPersonne1, testPersonne2));

        // When
        List<Personne> result = personneService.findAllPersonne();

        // Then
        assertThat(result).hasSize(2);
        verify(personneDao).findAll();
        verify(mapPersonnes, times(2)).putIfAbsent(anyLong(), any(Personne.class));
    }

    @Test
    @DisplayName("findAllPersonne() doit retourner une liste vide si aucune personne")
    void findAllPersonne_ShouldReturnEmptyListIfNoPersonnes() {
        // Given
        when(mapPersonnes.values()).thenReturn(Collections.emptyList());
        when(personneDao.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Personne> result = personneService.findAllPersonne();

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Tests de sauvegarde ====================

    @Test
    @DisplayName("savePersonne() doit sauvegarder et retourner l'ID")
    void savePersonne_ShouldSaveAndReturnId() {
        // Given
        Personne newPersonne = new Personne();
        newPersonne.setNom("TOM HANKS");

        Personne savedPersonne = new Personne();
        savedPersonne.setId(10L);
        savedPersonne.setNom("TOM HANKS");

        when(personneDao.save(newPersonne)).thenReturn(savedPersonne);

        // When
        Long result = personneService.savePersonne(newPersonne);

        // Then
        assertThat(result).isEqualTo(10L);
        verify(personneDao).save(newPersonne);
        verify(mapPersonnes).put(10L, savedPersonne);
    }

    @Test
    @DisplayName("savePersonne() doit mettre à jour le cache")
    void savePersonne_ShouldUpdateCache() {
        // Given
        when(personneDao.save(testPersonne1)).thenReturn(testPersonne1);

        // When
        personneService.savePersonne(testPersonne1);

        // Then
        verify(mapPersonnes).put(testPersonne1.getId(), testPersonne1);
    }

    // ==================== Tests de mise à jour ====================

    @Test
    @DisplayName("updatePersonne() doit mettre à jour la personne et le cache")
    void updatePersonne_ShouldUpdatePersonneAndCache() {
        // Given
        testPersonne1.setNom("LEONARDO DICAPRIO UPDATED");
        when(personneDao.save(testPersonne1)).thenReturn(testPersonne1);

        // When
        personneService.updatePersonne(testPersonne1);

        // Then
        verify(personneDao).save(testPersonne1);
        verify(mapPersonnes).put(testPersonne1.getId(), testPersonne1);
    }

    @Test
    @DisplayName("updatePersonne() ne doit rien faire si personne est null")
    void updatePersonne_ShouldDoNothingIfNull() {
        // When
        personneService.updatePersonne(null);

        // Then
        verify(personneDao, never()).save(any());
        verify(mapPersonnes, never()).put(anyLong(), any());
    }

    // ==================== Tests de recherche par nom ====================

    @Nested
    @DisplayName("Tests de findPersonneByName()")
    class FindPersonneByNameTests {

        @Test
        @DisplayName("doit retourner la personne depuis le cache")
        void findPersonneByName_ShouldReturnFromCache() {
            // Given
            Collection<Personne> cachedResults = Collections.singletonList(testPersonne1);
            when(mapPersonnes.values(any(Predicate.class))).thenReturn(cachedResults);

            // When
            Personne result = personneService.findPersonneByName("LEONARDO DICAPRIO");

            // Then
            assertThat(result).isEqualTo(testPersonne1);
            verify(mapPersonnes).values(any(Predicate.class));
            verify(personneDao, never()).findPersonneByNom(anyString());
        }

        @Test
        @DisplayName("doit retourner la personne depuis la DB si absente du cache")
        void findPersonneByName_ShouldReturnFromDatabaseIfNotInCache() {
            // Given
            when(mapPersonnes.values(any(Predicate.class))).thenReturn(Collections.emptyList());
            when(personneDao.findPersonneByNom("LEONARDO DICAPRIO")).thenReturn(testPersonne1);

            // When
            Personne result = personneService.findPersonneByName("LEONARDO DICAPRIO");

            // Then
            assertThat(result).isEqualTo(testPersonne1);
            verify(personneDao).findPersonneByNom("LEONARDO DICAPRIO");
            verify(mapPersonnes).put(testPersonne1.getId(), testPersonne1);
        }

        @Test
        @DisplayName("doit retourner null si la personne n'existe pas")
        void findPersonneByName_ShouldReturnNullIfNotExists() {
            // Given
            when(mapPersonnes.values(any(Predicate.class))).thenReturn(Collections.emptyList());
            when(personneDao.findPersonneByNom("UNKNOWN")).thenReturn(null);

            // When
            Personne result = personneService.findPersonneByName("UNKNOWN");

            // Then
            assertThat(result).isNull();
            verify(personneDao).findPersonneByNom("UNKNOWN");
            verify(mapPersonnes, never()).put(anyLong(), any());
        }
    }

    @Test
    @DisplayName("attachSessionPersonneByName() doit retourner la personne depuis la DB")
    void attachSessionPersonneByName_ShouldReturnFromDatabase() {
        // Given
        when(personneDao.findPersonneByNom("LEONARDO DICAPRIO")).thenReturn(testPersonne1);

        // When
        Personne result = personneService.attachSessionPersonneByName("LEONARDO DICAPRIO");

        // Then
        assertThat(result).isEqualTo(testPersonne1);
        verify(personneDao).findPersonneByNom("LEONARDO DICAPRIO");
    }

    // ==================== Tests de construction ====================

    @Test
    @DisplayName("buildPersonne() doit créer une personne avec nom et profilePath")
    void buildPersonne_ShouldCreatePersonneWithNameAndProfilePath() {
        // When
        Personne result = personneService.buildPersonne("TOM CRUISE", "/profile.jpg");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("TOM CRUISE");
        assertThat(result.getProfilePath()).isEqualTo("/profile.jpg");
    }

    @Test
    @DisplayName("buildPersonne() doit créer une personne sans profilePath si null")
    void buildPersonne_ShouldCreatePersonneWithoutProfilePathIfNull() {
        // When
        Personne result = personneService.buildPersonne("TOM CRUISE", null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("TOM CRUISE");
        assertThat(result.getProfilePath()).isNull();
    }

    @Test
    @DisplayName("buildPersonne() doit créer une personne sans profilePath si vide")
    void buildPersonne_ShouldCreatePersonneWithoutProfilePathIfEmpty() {
        // When
        Personne result = personneService.buildPersonne("TOM CRUISE", "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("TOM CRUISE");
        assertThat(result.getProfilePath()).isNull();
    }

    // ==================== Tests de createOrRetrievePersonne() ====================

    @Test
    @DisplayName("createOrRetrievePersonne() doit créer une nouvelle personne si elle n'existe pas")
    void createOrRetrievePersonne_ShouldCreateNewPersonneIfNotExists() {
        // Given
        when(personneDao.findPersonneByNom("NEW ACTOR")).thenReturn(null);

        Personne savedPersonne = new Personne();
        savedPersonne.setId(100L);
        savedPersonne.setNom("NEW ACTOR");
        savedPersonne.setProfilePath("/new.jpg");

        when(personneDao.save(any(Personne.class))).thenReturn(savedPersonne);

        // When
        Personne result = personneService.createOrRetrievePersonne("NEW ACTOR", "/new.jpg");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getNom()).isEqualTo("NEW ACTOR");
        verify(personneDao).findPersonneByNom("NEW ACTOR");
        verify(personneDao).save(any(Personne.class));
        verify(mapPersonnes).put(100L, savedPersonne);
    }

    @Test
    @DisplayName("createOrRetrievePersonne() doit retourner la personne existante")
    void createOrRetrievePersonne_ShouldReturnExistingPersonne() {
        // Given
        when(personneDao.findPersonneByNom("LEONARDO DICAPRIO")).thenReturn(testPersonne1);

        // When
        Personne result = personneService.createOrRetrievePersonne("LEONARDO DICAPRIO", "/profile.jpg");

        // Then
        assertThat(result).isEqualTo(testPersonne1);
        verify(personneDao).findPersonneByNom("LEONARDO DICAPRIO");
        verify(personneDao, never()).save(any());
    }

    // ==================== Tests de suppression ====================

    @Test
    @DisplayName("cleanAllPersonnes() doit vider le cache et la DB")
    void cleanAllPersonnes_ShouldClearCacheAndDatabase() {
        // When
        personneService.cleanAllPersonnes();

        // Then
        verify(mapPersonnes).clear();
        verify(personneDao).deleteAll();
    }

    @Test
    @DisplayName("cleanAllCaches() doit vider le cache")
    void cleanAllCaches_ShouldClearCache() {
        // When
        personneService.cleanAllCaches();

        // Then
        verify(mapPersonnes).clear();
    }

    // ==================== Tests utilitaires ====================

    @Nested
    @DisplayName("Tests de printPersonnes()")
    class PrintPersonnesTests {

        @Test
        @DisplayName("doit formater les noms avec le séparateur")
        void printPersonnes_ShouldFormatNamesWithSeparator() {
            // Given
            Set<Personne> personnes = new LinkedHashSet<>();
            personnes.add(testPersonne1);
            personnes.add(testPersonne2);
            personnes.add(testPersonne3);

            // When
            String result = personneService.printPersonnes(personnes, ", ");

            // Then
            assertThat(result).contains("LEONARDO DICAPRIO");
            assertThat(result).contains("CHRISTOPHER NOLAN");
            assertThat(result).contains("BRAD PITT");
            assertThat(result).contains(", ");
            assertThat(result).doesNotEndWith(", ");
        }

        @Test
        @DisplayName("doit retourner une chaîne vide si le set est vide")
        void printPersonnes_ShouldReturnEmptyStringIfSetIsEmpty() {
            // Given
            Set<Personne> personnes = new HashSet<>();

            // When
            String result = personneService.printPersonnes(personnes, ", ");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("doit retourner une chaîne vide si le set est null")
        void printPersonnes_ShouldReturnEmptyStringIfSetIsNull() {
            // When
            String result = personneService.printPersonnes(null, ", ");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("doit utiliser le séparateur personnalisé")
        void printPersonnes_ShouldUseCustomSeparator() {
            // Given
            Set<Personne> personnes = new LinkedHashSet<>();
            personnes.add(testPersonne1);
            personnes.add(testPersonne2);

            // When
            String result = personneService.printPersonnes(personnes, " | ");

            // Then
            assertThat(result).contains(" | ");
        }
    }

    // ==================== Tests d'intégration cache/DB ====================

    @Nested
    @DisplayName("Tests d'intégration Cache/Database")
    class CacheIntegrationTests {

        @Test
        @DisplayName("doit maintenir la cohérence entre cache et DB lors de la sauvegarde")
        void shouldMaintainConsistencyBetweenCacheAndDbOnSave() {
            // Given
            Personne newPersonne = new Personne();
            newPersonne.setNom("MERYL STREEP");

            Personne savedPersonne = new Personne();
            savedPersonne.setId(50L);
            savedPersonne.setNom("MERYL STREEP");

            when(personneDao.save(newPersonne)).thenReturn(savedPersonne);

            // When
            Long id = personneService.savePersonne(newPersonne);

            // Then
            verify(personneDao).save(newPersonne);
            verify(mapPersonnes).put(50L, savedPersonne);
            assertThat(id).isEqualTo(50L);
        }

        @Test
        @DisplayName("doit charger de la DB et mettre en cache lors de la première lecture")
        void shouldLoadFromDbAndCacheOnFirstRead() {
            // Given
            when(mapPersonnes.get(1L)).thenReturn(null);
            when(personneDao.findById(1L)).thenReturn(Optional.of(testPersonne1));

            // When
            Personne firstRead = personneService.findByPersonneId(1L);

            // Then - première lecture depuis DB
            verify(personneDao).findById(1L);
            verify(mapPersonnes).put(1L, testPersonne1);

            // Given - mise en cache simulée
            when(mapPersonnes.get(1L)).thenReturn(testPersonne1);

            // When - deuxième lecture
            Personne secondRead = personneService.findByPersonneId(1L);

            // Then - lecture depuis cache, pas de nouvel appel DB
            verify(personneDao, times(1)).findById(1L); // Toujours 1 seul appel
            assertThat(secondRead).isEqualTo(firstRead);
        }
    }

    // ==================== Tests de performance/edge cases ====================

    @Test
    @DisplayName("findAllPersonne() doit gérer une grande collection")
    void findAllPersonne_ShouldHandleLargeCollection() {
        // Given
        List<Personne> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Personne p = new Personne();
            p.setId((long) i);
            p.setNom("ACTOR_" + String.format("%04d", i));
            largeList.add(p);
        }
        when(mapPersonnes.values()).thenReturn(largeList);

        // When
        List<Personne> result = personneService.findAllPersonne();

        // Then
        assertThat(result).hasSize(1000);
        // Vérifier le tri
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getNom())
                    .isLessThanOrEqualTo(result.get(i + 1).getNom());
        }
    }

    @Test
    @DisplayName("updatePersonne() doit gérer les modifications multiples")
    void updatePersonne_ShouldHandleMultipleUpdates() {
        // Given
        when(personneDao.save(any(Personne.class))).thenReturn(testPersonne1);

        // When
        testPersonne1.setNom("FIRST UPDATE");
        personneService.updatePersonne(testPersonne1);

        testPersonne1.setNom("SECOND UPDATE");
        personneService.updatePersonne(testPersonne1);

        // Then
        verify(personneDao, times(2)).save(testPersonne1);
        verify(mapPersonnes, times(2)).put(testPersonne1.getId(), testPersonne1);
    }
}
