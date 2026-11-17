package io.quarkiverse.modulith.runtime.events;

/**
 * Carries a checked exception of a listener through the transaction runner.
 */
final class ListenerFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ListenerFailure(Throwable cause) {
        super(cause);
    }
}
