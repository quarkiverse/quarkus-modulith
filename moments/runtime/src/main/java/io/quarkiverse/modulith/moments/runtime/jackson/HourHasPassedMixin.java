package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.HourHasPassed;

/**
 * Jackson mix-in that creates {@link HourHasPassed} through its factory method.
 */
interface HourHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static HourHasPassed of(@JsonProperty("time") LocalDateTime time) {
        return HourHasPassed.of(time);
    }
}
