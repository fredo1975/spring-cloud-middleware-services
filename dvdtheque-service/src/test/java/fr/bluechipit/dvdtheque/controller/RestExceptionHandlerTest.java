package fr.bluechipit.dvdtheque.controller;

import api.error.ApiError;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("RestExceptionHandler - Unit Tests")
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    @DisplayName("handleHttpMessageNotReadable doit produire un ApiError 400")
    void notReadable_ShouldReturnBadRequest() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = (ApiError) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Malformed JSON request");
    }

    @Test
    @DisplayName("handleEntityNotFound doit produire un ApiError 404 avec le message")
    void entityNotFound_ShouldReturnNotFound() {
        ResponseEntity<Object> response = handler.handleEntityNotFound(new EntityNotFoundException("Film absent"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError body = (ApiError) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Film absent");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleAccessDeniedException doit produire un ApiError 403")
    void accessDenied_ShouldReturnForbidden() {
        ResponseEntity<Object> response =
                handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ApiError body = (ApiError) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("denied");
        assertThat(body.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
