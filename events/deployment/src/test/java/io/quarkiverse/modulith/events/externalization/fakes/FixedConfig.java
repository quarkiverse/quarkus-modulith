package io.quarkiverse.modulith.events.externalization.fakes;

import java.time.Duration;

import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;

public class FixedConfig implements ExternalizationRuntimeConfig {

    private final boolean serialize;
    private final FixedOutbox outbox;

    public FixedConfig(boolean serialize, Duration timeout) {
        this(serialize, new FixedOutbox(timeout, Duration.ofMinutes(1), 100));
    }

    public FixedConfig(boolean serialize, FixedOutbox outbox) {
        this.serialize = serialize;
        this.outbox = outbox;
    }

    @Override
    public boolean serializeExternalization() {
        return serialize;
    }

    @Override
    public Duration serializedSendTimeout() {
        return outbox.sendTimeout();
    }

    @Override
    public Outbox outbox() {
        return outbox;
    }
}
