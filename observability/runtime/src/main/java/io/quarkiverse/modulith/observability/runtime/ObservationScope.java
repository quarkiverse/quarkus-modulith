package io.quarkiverse.modulith.observability.runtime;

/**
 * An observation of one module invocation, started by an {@link InvocationObserver}.
 */
public interface ObservationScope {

    /**
     * Records that the invocation failed.
     */
    void error(Throwable error);

    /**
     * Ends the observation.
     */
    void stop();
}
