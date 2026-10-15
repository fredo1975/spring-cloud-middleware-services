package fr.bluechipit.dvdtheque.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    private static void sample(String value) {
    }

    @Test
    @DisplayName("handleResourceNotFound doit produire un ProblemDetail 404")
    void resourceNotFound_ShouldReturnNotFoundProblem() {
        ProblemDetail problem = handler.handleResourceNotFound(new ResourceNotFoundException("absent"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("absent");
        assertThat(problem.getTitle()).isEqualTo("Resource not found");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleValidationExceptions doit agréger les erreurs de champ en 400")
    void validation_ShouldAggregateFieldErrors() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "film");
        binding.addError(new FieldError("film", "titre", "ne doit pas etre nul"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new MethodParameter(method, -1), binding);

        ProblemDetail problem = handler.handleValidationExceptions(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("titre: ne doit pas etre nul");
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
    }

    @Test
    @DisplayName("handleGeneralException doit produire un ProblemDetail 500")
    void generalException_ShouldReturnInternalServerError() {
        ProblemDetail problem = handler.handleGeneralException(new RuntimeException("boom"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An internal error occured : boom");
    }
}
