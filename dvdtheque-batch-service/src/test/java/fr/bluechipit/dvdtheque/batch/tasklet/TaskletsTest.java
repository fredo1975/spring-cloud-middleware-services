package fr.bluechipit.dvdtheque.batch.tasklet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import static org.assertj.core.api.Assertions.assertThat;

class TaskletsTest {

    @Test
    @DisplayName("CopyFileTasklet finishes immediately")
    void copyFileTasklet_ShouldFinish() throws Exception {
        assertThat(new CopyFileTasklet().execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    @DisplayName("RetrieveDateInsertionTasklet finishes immediately")
    void retrieveDateInsertionTasklet_ShouldFinish() throws Exception {
        assertThat(new RetrieveDateInsertionTasklet().execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    @DisplayName("RippedFlagTasklet finishes immediately")
    void rippedFlagTasklet_ShouldFinish() throws Exception {
        assertThat(new RippedFlagTasklet().execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    @DisplayName("TheMovieDbTasklet finishes immediately")
    void theMovieDbTasklet_ShouldFinish() throws Exception {
        assertThat(new TheMovieDbTasklet().execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
    }
}
