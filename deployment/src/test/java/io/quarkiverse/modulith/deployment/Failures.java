package io.quarkiverse.modulith.deployment;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.Violations;

/**
 * Reads the build failures the extension tests assert on.
 */
public final class Failures {

    private Failures() {
    }

    /**
     * @return the messages of the exception chain, one per line, those of suppressed exceptions included
     */
    public static String messages(Throwable failure) {
        return chain(failure).map(it -> it.getMessage() + "\n" + Stream.of(it.getSuppressed()).map(Failures::messages)
                .collect(Collectors.joining())).collect(Collectors.joining());
    }

    /**
     * @return the first {@link Violations} of the exception chain
     */
    public static Optional<Violations> violations(Throwable failure) {
        return chain(failure).filter(Violations.class::isInstance).map(Violations.class::cast).findFirst();
    }

    private static Stream<Throwable> chain(Throwable failure) {
        return Stream.iterate(failure, Objects::nonNull, Throwable::getCause);
    }
}
