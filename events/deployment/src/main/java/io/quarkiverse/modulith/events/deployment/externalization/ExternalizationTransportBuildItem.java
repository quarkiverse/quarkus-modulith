package io.quarkiverse.modulith.events.deployment.externalization;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced by an extension that provides an {@code EventExternalizationTransport} bean, such as
 * {@code quarkus-modulith-events-messaging}. Externalization is only active when a transport exists.
 */
public final class ExternalizationTransportBuildItem extends MultiBuildItem {

    private final String name;

    public ExternalizationTransportBuildItem(String name) {
        this.name = name;
    }

    /**
     * @return a name of the transport for messages, such as {@code messaging}
     */
    public String getName() {
        return name;
    }
}
