package exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exceptions du socle commun Unit Tests")
class CommonsExceptionsTest {

    @Test
    @DisplayName("DvdthequeCommonsException doit porter un message")
    void commonsException_ShouldCarryMessage() {
        DvdthequeCommonsException ex = new DvdthequeCommonsException("boom");

        assertThat(ex).isInstanceOf(Exception.class);
        assertThat(ex).hasMessage("boom");
    }

    @Test
    @DisplayName("DvdthequeCommonsException(Exception) doit utiliser l'exception comme cause et message")
    void commonsException_FromExceptionShouldCarryCause() {
        IllegalStateException cause = new IllegalStateException("cause");

        DvdthequeCommonsException ex = new DvdthequeCommonsException(cause);

        assertThat(ex).hasCause(cause);
        assertThat(ex).hasMessageContaining("cause");
    }

    @Test
    @DisplayName("DvdthequeCommonsException(String, Throwable) doit porter message et cause")
    void commonsException_ShouldCarryMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("cause");

        DvdthequeCommonsException ex = new DvdthequeCommonsException("boom", cause);

        assertThat(ex).hasMessage("boom").hasCause(cause);
    }

    @Test
    @DisplayName("NotImplementedException doit être annotée 501 NOT_IMPLEMENTED")
    void notImplementedException_ShouldBeAnnotated() {
        org.springframework.web.bind.annotation.ResponseStatus responseStatus =
                NotImplementedException.class.getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.code()).isEqualTo(org.springframework.http.HttpStatus.NOT_IMPLEMENTED);
        assertThat(responseStatus.reason()).isEqualTo("error.not.implemented");
    }

    @Test
    @DisplayName("ParameterException doit être annotée 400 BAD_REQUEST")
    void parameterException_ShouldBeAnnotated() {
        org.springframework.web.bind.annotation.ResponseStatus responseStatus =
                ParameterException.class.getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.code()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(responseStatus.reason()).isEqualTo("error.parameter");
    }
}
