package utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DateUtils Unit Tests")
class DateUtilsTest {

    @Test
    @DisplayName("Le pattern TMDB doit être yyyy-MM-dd")
    void tmdbDatePatternShouldBeIsoDate() {
        assertThat(DateUtils.TMDB_DATE_PATTERN).isEqualTo("yyyy-MM-dd");
    }

    @Test
    @DisplayName("clearDate() doit remettre heure/minute/seconde/milliseconde à zéro en conservant la date")
    void clearDate_ShouldZeroTimeFieldsAndKeepDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.MARCH, 15, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 123);

        Date cleared = DateUtils.clearDate(cal.getTime());

        Calendar result = Calendar.getInstance();
        result.setTime(cleared);
        assertThat(result.get(Calendar.YEAR)).isEqualTo(2021);
        assertThat(result.get(Calendar.MONTH)).isEqualTo(Calendar.MARCH);
        assertThat(result.get(Calendar.DAY_OF_MONTH)).isEqualTo(15);
        assertThat(result.get(Calendar.HOUR_OF_DAY)).isZero();
        assertThat(result.get(Calendar.MINUTE)).isZero();
        assertThat(result.get(Calendar.SECOND)).isZero();
        assertThat(result.get(Calendar.MILLISECOND)).isZero();
    }

    @Test
    @DisplayName("clearDate() quirk: lève une NPE pour une date nulle")
    void clearDate_ShouldThrowNullPointerForNullDate() {
        assertThatThrownBy(() -> DateUtils.clearDate(null)).isInstanceOf(NullPointerException.class);
    }
}
