package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Unwraps the failures of asynchronous sends.
 */
final class Failures {

    private Failures() {
    }

    /**
     * @return the cause of a {@link CompletionException} or {@link ExecutionException}, the failure itself otherwise
     */
    static Throwable unwrap(Throwable failure) {
        boolean wrapper = failure instanceof CompletionException || failure instanceof ExecutionException;
        return wrapper && Objects.nonNull(failure.getCause()) ? failure.getCause() : failure;
    }
}
