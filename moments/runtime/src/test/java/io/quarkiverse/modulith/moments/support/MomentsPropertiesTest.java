package io.quarkiverse.modulith.moments.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.moments.Quarter;
import io.quarkiverse.modulith.moments.ShiftedQuarter;

class MomentsPropertiesTest {

    static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void defaultsToTheSettingsOfSpringModulith() {
        // act
        MomentsProperties properties = MomentsProperties.defaults();

        // assert
        assertThat(properties.getGranularity()).isEqualTo(Granularity.HOURS);
        assertThat(properties.getZoneId()).isEqualTo(ZoneOffset.UTC);
        assertThat(properties.getLocale()).isEqualTo(Locale.getDefault());
        assertThat(properties.getQuarterStartMonth()).isEqualTo(Month.JANUARY);
        assertThat(properties.isEnableTimeMachine()).isFalse();
    }

    @Test
    void keepsExplicitValues() {
        // act
        MomentsProperties properties = MomentsProperties.of(Granularity.MINUTES, BERLIN, Locale.US, Month.APRIL, true);

        // assert
        assertThat(properties.getGranularity()).isEqualTo(Granularity.MINUTES);
        assertThat(properties.getZoneId()).isEqualTo(BERLIN);
        assertThat(properties.getLocale()).isEqualTo(Locale.US);
        assertThat(properties.getQuarterStartMonth()).isEqualTo(Month.APRIL);
        assertThat(properties.isEnableTimeMachine()).isTrue();
        assertThat(properties).hasToString("MomentsProperties[granularity=MINUTES, zoneId=Europe/Berlin, locale=en-US, "
                + "quarterStartMonth=APRIL, enableTimeMachine=true]");
    }

    @Test
    void copiesWithOneValueChanged() {
        // arrange
        MomentsProperties properties = MomentsProperties.of(Granularity.MINUTES, BERLIN, Locale.US, Month.APRIL, true);

        // act
        MomentsProperties granularity = properties.withGranularity(Granularity.DAYS);
        MomentsProperties zone = properties.withZoneId(ZoneOffset.UTC);
        MomentsProperties locale = properties.withLocale(Locale.GERMANY);
        MomentsProperties quarterStartMonth = properties.withQuarterStartMonth(Month.OCTOBER);

        // assert
        assertThat(granularity).hasToString("MomentsProperties[granularity=DAYS, zoneId=Europe/Berlin, locale=en-US, "
                + "quarterStartMonth=APRIL, enableTimeMachine=true]");
        assertThat(zone).hasToString("MomentsProperties[granularity=MINUTES, zoneId=Z, locale=en-US, "
                + "quarterStartMonth=APRIL, enableTimeMachine=true]");
        assertThat(locale).hasToString("MomentsProperties[granularity=MINUTES, zoneId=Europe/Berlin, locale=de-DE, "
                + "quarterStartMonth=APRIL, enableTimeMachine=true]");
        assertThat(quarterStartMonth).hasToString("MomentsProperties[granularity=MINUTES, zoneId=Europe/Berlin, "
                + "locale=en-US, quarterStartMonth=OCTOBER, enableTimeMachine=true]");
    }

    static Stream<Arguments> quarters() {
        return Stream.of(
                arguments(Month.JANUARY, LocalDate.of(2022, 1, 1), ShiftedQuarter.of(Quarter.Q1, Month.JANUARY)),
                arguments(Month.FEBRUARY, LocalDate.of(2022, 1, 1), ShiftedQuarter.of(Quarter.Q4, Month.FEBRUARY)),
                arguments(Month.FEBRUARY, LocalDate.of(2022, 7, 31), ShiftedQuarter.of(Quarter.Q2, Month.FEBRUARY)),
                arguments(Month.APRIL, LocalDate.of(2026, 3, 31), ShiftedQuarter.of(Quarter.Q4, Month.APRIL)),
                arguments(Month.APRIL, LocalDate.of(2026, 4, 1), ShiftedQuarter.of(Quarter.Q1, Month.APRIL)),
                arguments(Month.OCTOBER, LocalDate.of(2026, 12, 31), ShiftedQuarter.of(Quarter.Q1, Month.OCTOBER)),
                arguments(Month.DECEMBER, LocalDate.of(2028, 2, 29), ShiftedQuarter.of(Quarter.Q1, Month.DECEMBER)));
    }

    @ParameterizedTest
    @MethodSource("quarters")
    void findsTheShiftedQuarterOfADate(Month startMonth, LocalDate date, ShiftedQuarter expected) {
        // arrange
        MomentsProperties properties = MomentsProperties.defaults().withQuarterStartMonth(startMonth);

        // act
        ShiftedQuarter quarter = properties.getShiftedQuarter(date);

        // assert
        assertThat(quarter).isEqualTo(expected);
    }

    static Stream<Arguments> nullValues() {
        return Stream.of(
                arguments((ThrowingCallable) () -> MomentsProperties.defaults().withGranularity(null),
                        "Granularity must not be null!"),
                arguments((ThrowingCallable) () -> MomentsProperties.defaults().withZoneId(null),
                        "ZoneId must not be null!"),
                arguments((ThrowingCallable) () -> MomentsProperties.defaults().withLocale(null),
                        "Locale must not be null!"),
                arguments((ThrowingCallable) () -> MomentsProperties.defaults().withQuarterStartMonth(null),
                        "Quarter start month must not be null!"),
                arguments((ThrowingCallable) () -> MomentsProperties.defaults().getShiftedQuarter(null),
                        "Reference date must not be null!"));
    }

    @ParameterizedTest
    @MethodSource("nullValues")
    void rejectsNullValues(ThrowingCallable call, String message) {
        // act
        Throwable thrown = catchThrowable(call);

        // assert
        assertThat(thrown).isExactlyInstanceOf(NullPointerException.class).hasMessage(message);
    }
}
