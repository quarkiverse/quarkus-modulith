package io.quarkiverse.modulith.events.messaging.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * The outgoing channels of event externalization. Quarkus Messaging channels are static, so the build generates a
 * subclass with one injected {@link Emitter} per channel, which it registers here once injected.
 */
public abstract class MessagingChannels {

    private final Map<String, Emitter<Object>> emitters = new TreeMap<>();
    private final String defaultChannel;

    /**
     * @param defaultChannel the channel of targets without a channel of their own, {@code null} for none
     */
    protected MessagingChannels(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    /**
     * Registers the emitter of a channel.
     */
    public void register(String channel, Emitter<Object> emitter) {
        emitters.put(channel, emitter);
    }

    /**
     * @return the emitter of the channel named like the routing target
     */
    public Optional<Emitter<Object>> channel(String target) {
        return Optional.ofNullable(emitters.get(target));
    }

    /**
     * @return the emitter of the default channel, if one is configured
     */
    public Optional<Emitter<Object>> defaultChannel() {
        return Optional.ofNullable(defaultChannel).map(emitters::get);
    }

    /**
     * @return the names of all channels, in alphabetical order, read-only
     */
    public Set<String> names() {
        return Collections.unmodifiableSet(emitters.keySet());
    }
}
