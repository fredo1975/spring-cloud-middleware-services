package fr.bluechipit.dvdtheque.allocine.controller;

import fr.bluechipit.dvdtheque.allocine.service.AllocineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksTest {

    @Mock
    private AllocineService allocineService;

    @InjectMocks
    private ScheduledTasks scheduledTasks;

    @Test
    @DisplayName("the scheduled task delegates to the multithreaded scraping exactly once")
    void retrieveAllocineScrapingFicheFilm_DelegatesToService() {
        scheduledTasks.retrieveAllocineScrapingFicheFilm();

        verify(allocineService, times(1)).scrapAllAllocineFicheFilmMultithreaded();
        verifyNoMoreInteractions(allocineService);
    }
}
