package fr.bluechipit.dvdtheque.batch.launcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class ImportFilmsLauncherTest {

    @Test
    @DisplayName("launchCsvFileToDatabaseJob completes without side effects")
    void launchCsvFileToDatabaseJob_ShouldComplete() {
        ImportFilmsLauncher launcher = new ImportFilmsLauncher(mock(Job.class), mock(JobLauncher.class));

        assertThatCode(launcher::launchCsvFileToDatabaseJob).doesNotThrowAnyException();
    }
}
