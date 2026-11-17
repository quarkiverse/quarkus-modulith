package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.Year;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.QuarterHasPassed;
import io.quarkiverse.modulith.moments.ShiftedQuarter;

/**
 * Jackson mix-in that creates {@link QuarterHasPassed} through its factory method and leaves the derived start and end
 * dates out.
 */
@JsonIgnoreProperties({ "startDate", "endDate" })
interface QuarterHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static QuarterHasPassed of(@JsonProperty("year") Year year, @JsonProperty("quarter") ShiftedQuarter quarter) {
        return QuarterHasPassed.of(year, quarter);
    }
}
