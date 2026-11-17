package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class RecordingDrain extends OutboxDrain {

    final List<String> calls = new ArrayList<>();

    @Override
    public void trigger() {
        calls.add("trigger");
    }

    @Override
    boolean send(UUID publicationId, Object event) {
        calls.add("send " + event);
        return true;
    }
}
