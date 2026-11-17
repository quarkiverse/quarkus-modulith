package io.quarkiverse.modulith.junit;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Whether to execute or skip all tests when changes were detected, but none to a class, class path resource or build
 * file. Set with {@code quarkus.modulith.test.on-no-changes}.
 */
enum OnNoChange {

    EXECUTE_ALL("execute-all"),

    /**
     * {@code execute-none} is accepted too, the value Spring Modulith's configuration metadata advertises.
     */
    SKIP_ALL("skip-all", "execute-none");

    private final List<String> values;

    OnNoChange(String... values) {
        this.values = List.of(values);
    }

    /**
     * @param value the configured value, compared ignoring case
     * @return {@link #EXECUTE_ALL} if absent
     * @throws IllegalArgumentException for an unknown value
     */
    static OnNoChange fromConfig(Optional<String> value) {
        return value.map(OnNoChange::parse).orElse(EXECUTE_ALL);
    }

    private static OnNoChange parse(String value) {
        return Stream.of(values())
                .filter(it -> it.values.stream().anyMatch(value::equalsIgnoreCase))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid on-no-changes value '%s'! Needs to be any of %s!"
                        .formatted(value, Stream.of(values()).flatMap(it -> it.values.stream()).toList())));
    }
}
