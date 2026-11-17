package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.Year;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.YearHasPassed;

/**
 * Jackson mix-in that creates {@link YearHasPassed} through its factory method.
 */
interface YearHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static YearHasPassed of(@JsonProperty("year") Year year) {
        return YearHasPassed.of(year);
    }
}
