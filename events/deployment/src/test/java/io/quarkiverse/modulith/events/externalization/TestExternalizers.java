package io.quarkiverse.modulith.events.externalization;

import java.time.Duration;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.EventExternalized;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.fakes.DirectExecutor;
import io.quarkiverse.modulith.events.externalization.fakes.FakeTransport;
import io.quarkiverse.modulith.events.externalization.fakes.FixedConfig;
import io.quarkiverse.modulith.events.externalization.fakes.RecordingEvent;
import io.quarkiverse.modulith.events.externalization.runtime.RoutingResolver;

public final class TestExternalizers {

    public static final EventExternalizationConfiguration STRINGS = EventExternalizationConfiguration.externalizing()
            .selectByType(String.class).routeAll(event -> RoutingTarget.forTarget("strings")).build();

    public static final EventExternalizationConfiguration SERIALIZED_STRINGS = EventExternalizationConfiguration
            .externalizing().selectByType(String.class).routeAll(event -> RoutingTarget.forTarget("strings"))
            .serializeExternalization().build();

    private TestExternalizers() {
    }

    /**
     * @return an externalizer that sends one event at a time
     */
    public static EventExternalizer serialized(FakeTransport transport) {
        EventExternalizer externalizer = of(transport);
        externalizer.configuration = SERIALIZED_STRINGS;
        externalizer.runtimeConfig = new FixedConfig(true, Duration.ofSeconds(1));
        return externalizer;
    }

    public static EventExternalizer of(FakeTransport transport) {
        return of(transport, new RecordingEvent<EventExternalized<Object, Object>>(null));
    }

    public static EventExternalizer of(FakeTransport transport, RecordingEvent<EventExternalized<Object, Object>> fired) {
        EventExternalizer externalizer = new EventExternalizer();
        externalizer.configuration = STRINGS;
        externalizer.transport = transport;
        externalizer.routing = new RoutingResolver();
        externalizer.events = fired;
        externalizer.executor = new DirectExecutor();
        externalizer.runtimeConfig = new FixedConfig(false, Duration.ofSeconds(1));
        return externalizer;
    }
}
