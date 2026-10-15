package fr.bluechipit.dvdtheque.batch.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledExportFilmsConfigurationTest {

    private final ScheduledExportFilmsConfiguration configuration = new ScheduledExportFilmsConfiguration();
    private final JobLauncher jobLauncher = mock(JobLauncher.class);
    private final Job job = mock(Job.class);

    private void wire() {
        ReflectionTestUtils.setField(configuration, "jobLauncher", jobLauncher);
        ReflectionTestUtils.setField(configuration, "job", job);
    }

    @Test
    @DisplayName("exportFilmsJob launches the export job with empty parameters")
    void exportFilmsJob_ShouldLaunchJob() throws Exception {
        wire();

        configuration.exportFilmsJob();

        verify(jobLauncher).run(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("exportFilmsJob swallows launch failures")
    void exportFilmsJob_WhenLaunchFails_ShouldNotThrow() throws Exception {
        wire();
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatCode(configuration::exportFilmsJob).doesNotThrowAnyException();
    }
}
