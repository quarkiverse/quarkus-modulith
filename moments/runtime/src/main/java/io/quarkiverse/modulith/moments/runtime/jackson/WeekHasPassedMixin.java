package io.quarkiverse.modulith.moments.runtime.jackson;

import java.time.Year;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkiverse.modulith.moments.WeekHasPassed;

/**
 * Jackson mix-in that creates {@link WeekHasPassed} through its factory method and writes its locale. The start and
 * end dates are written, as Spring Modulith does, and ignored when reading. The property order is fixed, because the
 * order in which the JVM lists the methods of a class is not.
 */
@JsonPropertyOrder({ "year", "week", "locale", "startDate", "endDate" })
@JsonIgnoreProperties(value = { "startDate", "endDate" }, allowGetters = true)
interface WeekHasPassedMixin {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static WeekHasPassed of(@JsonProperty("year") Year year, @JsonProperty("week") int week,
            @JsonProperty("locale") Locale locale) {
        return WeekHasPassed.of(year, week, locale);
    }

    @JsonGetter("locale")
    Locale getLocale();
}
