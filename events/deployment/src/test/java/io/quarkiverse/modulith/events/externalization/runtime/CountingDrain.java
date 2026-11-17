package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.List;

class CountingDrain extends OutboxDrain {

    private final List<String> runs;

    CountingDrain(List<String> runs) {
        this.runs = runs;
    }

    @Override
    void runOnce() {
        if (runs.isEmpty()) {
            runs.add("run 1 triggers again");
            trigger();
        } else {
            runs.add("run 2");
        }
    }
}
