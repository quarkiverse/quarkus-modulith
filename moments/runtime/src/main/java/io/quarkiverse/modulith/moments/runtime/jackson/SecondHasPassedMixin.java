package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.SecondHasPassed;

/**
 * Jackson mix-in that creates {@link SecondHasPassed} through its factory method.
 */
interface SecondHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static SecondHasPassed of(@JsonProperty("time") LocalDateTime time) {
        return SecondHasPassed.of(time);
    }
}
