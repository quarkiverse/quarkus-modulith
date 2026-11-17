package io.quarkiverse.modulith.observability.runtime;

import java.util.List;

/**
 * Records errors and stops.
 */
record RecordingScope(List<String> events) implements ObservationScope {

    @Override
    public void error(Throwable error) {
        events.add("error " + error.getMessage());
    }

    @Override
    public void stop() {
        events.add("stop");
    }
}
