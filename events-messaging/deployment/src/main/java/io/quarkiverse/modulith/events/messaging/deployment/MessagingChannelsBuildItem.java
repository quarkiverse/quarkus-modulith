package io.quarkiverse.modulith.events.messaging.deployment;

import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The outgoing channels the build generates for event externalization, empty when externalization is not active.
 */
public final class MessagingChannelsBuildItem extends SimpleBuildItem {

    private final Set<String> names;
    private final String defaultChannel;

    MessagingChannelsBuildItem(Set<String> names, Optional<String> defaultChannel) {
        this.names = Set.copyOf(names);
        this.defaultChannel = defaultChannel.orElse(null);
    }

    /**
     * @return the channel names, each named like its routing target, including the default channel
     */
    public Set<String> getNames() {
        return names;
    }

    /**
     * @return the channel of targets without a channel of their own
     */
    public Optional<String> getDefaultChannel() {
        return Optional.ofNullable(defaultChannel);
    }
}
