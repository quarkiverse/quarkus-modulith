package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.Month;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.modulith.moments.Quarter;
import io.quarkiverse.modulith.moments.ShiftedQuarter;

/**
 * Jackson mix-in that creates {@link ShiftedQuarter} through its factory method and leaves the derived start and end
 * days out.
 */
@JsonIgnoreProperties({ "start", "end" })
interface ShiftedQuarterMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static ShiftedQuarter of(@JsonProperty("quarter") Quarter quarter, @JsonProperty("startMonth") Month startMonth) {
        return ShiftedQuarter.of(quarter, startMonth);
    }
}
