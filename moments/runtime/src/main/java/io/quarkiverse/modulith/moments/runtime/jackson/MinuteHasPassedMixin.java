package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.MinuteHasPassed;

/**
 * Jackson mix-in that creates {@link MinuteHasPassed} through its factory method.
 */
interface MinuteHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static MinuteHasPassed of(@JsonProperty("time") LocalDateTime time) {
        return MinuteHasPassed.of(time);
    }
}
