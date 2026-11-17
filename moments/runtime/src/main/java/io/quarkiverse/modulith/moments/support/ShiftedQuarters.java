package io.quarkiverse.modulith.moments.support;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import io.quarkiverse.modulith.moments.Quarter;
import io.quarkiverse.modulith.moments.ShiftedQuarter;

/**
 * The four {@link ShiftedQuarter}s of a fiscal year starting in a given month.
 */
final class ShiftedQuarters {

    private final List<ShiftedQuarter> quarters;

    private ShiftedQuarters(List<ShiftedQuarter> quarters) {
        this.quarters = quarters;
    }

    static ShiftedQuarters of(Month startMonth) {
        return new ShiftedQuarters(Arrays.stream(Quarter.values())
                .map(quarter -> ShiftedQuarter.of(quarter, startMonth))
                .toList());
    }

    ShiftedQuarter getCurrent(LocalDate reference) {
        return quarters.stream()
                .filter(quarter -> quarter.contains(reference))
                .findFirst()
                .orElseThrow();
    }
}
