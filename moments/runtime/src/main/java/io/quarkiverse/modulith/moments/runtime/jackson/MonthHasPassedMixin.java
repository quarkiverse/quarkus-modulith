package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.MonthHasPassed;

/**
 * Jackson mix-in that creates {@link MonthHasPassed} through its factory method.
 */
interface MonthHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static MonthHasPassed of(@JsonProperty("month") YearMonth month) {
        return MonthHasPassed.of(month);
    }
}
