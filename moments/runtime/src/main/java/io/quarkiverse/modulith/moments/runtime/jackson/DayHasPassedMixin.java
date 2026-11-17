package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.DayHasPassed;

/**
 * Jackson mix-in that creates {@link DayHasPassed} through its factory method.
 */
interface DayHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static DayHasPassed of(@JsonProperty("date") LocalDate date) {
        return DayHasPassed.of(date);
    }
}
