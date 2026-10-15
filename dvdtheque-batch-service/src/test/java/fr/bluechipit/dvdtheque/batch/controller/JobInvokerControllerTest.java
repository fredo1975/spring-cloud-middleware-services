package fr.bluechipit.dvdtheque.batch.controller;

import fr.bluechipit.dvdtheque.batch.film.backup.MultipartFileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@ActiveProfiles("test-import")
@AutoConfigureMockMvc
class JobInvokerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean
    private MultipartFileUtil multipartFileUtil;

    @MockitoBean
    private AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    @Qualifier("runExportFilmsJob")
    private Job runExportFilmsJob;

    @Autowired
    @Qualifier("importFilmsJob")
    private Job importFilmsJob;

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("GET exportFilmsJob launches the export job")
    void handleExportFilmsJob_ShouldLaunchExportJob() throws Exception {
        mockMvc.perform(get("/dvdtheque-batch-service/invokejob/exportFilmsJob")
                        .with(jwt().jwt(builder -> builder.subject("test")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch exportFilmsJob has been invoked"));

        verify(jobLauncher).run(eq(runExportFilmsJob), any(JobParameters.class));
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("POST importFilmsJob materialises the file then launches the import job")
    void handleImportFilmsJob_ShouldLaunchImportJob() throws Exception {
        when(multipartFileUtil.createFileToImport(any())).thenReturn(new File("target/import.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "ListeDvd.csv", "text/csv", "a;b".getBytes());

        mockMvc.perform(multipart("/dvdtheque-batch-service/invokejob/importFilmsJob").file(file)
                        .with(jwt().jwt(builder -> builder.subject("test")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch importFilmsJob has been invoked"));

        verify(jobLauncher).run(eq(importFilmsJob), any(JobParameters.class));
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("POST films/import returns 204 when the import job is launched")
    void importFilmList_ShouldReturnNoContent() throws Exception {
        when(multipartFileUtil.createFileToImport(any())).thenReturn(new File("target/import.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "ListeDvd.csv", "text/csv", "a;b".getBytes());

        mockMvc.perform(multipart("/dvdtheque-batch-service/invokejob/films/import").file(file)
                        .with(jwt().jwt(builder -> builder.subject("test")))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(jobLauncher).run(eq(importFilmsJob), any(JobParameters.class));
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("POST films/import returns 500 when the file cannot be converted")
    void importFilmList_WhenConversionFails_ShouldReturnServerError() throws Exception {
        when(multipartFileUtil.createFileToImport(any())).thenThrow(new RuntimeException("bad file"));
        MockMultipartFile file = new MockMultipartFile("file", "broken.txt", "text/plain", "x".getBytes());

        mockMvc.perform(multipart("/dvdtheque-batch-service/invokejob/films/import").file(file)
                        .with(jwt().jwt(builder -> builder.subject("test")))
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }
}
