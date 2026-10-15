package fr.bluechipit.dvdtheque.tmdb.controller;

import api.error.ApiError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestExceptionHandler Unit Tests")
class RestExceptionHandlerTest {

	private final RestExceptionHandler handler = new RestExceptionHandler();

	@Test
	@DisplayName("handleServerError() doit retourner une ApiError 500 avec le message de l'exception")
	void handleServerError_ShouldReturnInternalServerError() {
		ResponseEntity<Object> response = handler.handleServerError(new RuntimeException("Boom"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		ApiError apiError = (ApiError) response.getBody();
		assertThat(apiError).isNotNull();
		assertThat(apiError.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(apiError.getMessage()).isEqualTo("Boom");
		assertThat(apiError.getTimestamp()).isNotNull();
	}

	@Test
	@DisplayName("handleHttpMessageNotReadable() doit retourner une ApiError 400 Malformed JSON request")
	void handleHttpMessageNotReadable_ShouldReturnBadRequest() {
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad json", null, null);

		ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(ex, null, null, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ApiError apiError = (ApiError) response.getBody();
		assertThat(apiError).isNotNull();
		assertThat(apiError.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(apiError.getMessage()).isEqualTo("Malformed JSON request");
	}
}
