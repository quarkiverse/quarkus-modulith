package io.quarkiverse.modulith.observability.runtime;

/**
 * An observation that fails to record errors and to stop.
 */
record FailingScope() implements ObservationScope {

    @Override
    public void error(Throwable error) {
        throw new IllegalStateException("cannot record");
    }

    @Override
    public void stop() {
        throw new IllegalStateException("cannot stop");
    }
}
