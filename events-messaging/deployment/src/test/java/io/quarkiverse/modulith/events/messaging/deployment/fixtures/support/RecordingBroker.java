package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.OutgoingMessageMetadata;

@Singleton
public class RecordingBroker {

    public static final String RESULT = "stored-by-the-broker";

    private final Map<String, List<Message<?>>> received = new ConcurrentHashMap<>();
    private final AtomicReference<BrokerMode> mode = new AtomicReference<>(BrokerMode.ACKNOWLEDGE);
    private final List<Throwable> failures = new CopyOnWriteArrayList<>();

    void receive(String channel, Message<?> message) {
        received.computeIfAbsent(channel, name -> new CopyOnWriteArrayList<>()).add(message);
        switch (mode.get()) {
            case ACKNOWLEDGE -> {
                OutgoingMessageMetadata.setResultOnMessage(message, RESULT);
                message.ack();
            }
            case REJECT -> message.nack(new IllegalStateException("broker down"));
            case SILENT -> {
            }
        }
    }

    void fail(Throwable failure) {
        failures.add(failure);
    }

    void complete(String channel) {
        failures.add(new IllegalStateException("channel " + channel + " completed"));
    }

    public List<Message<?>> received(String channel) {
        return List.copyOf(received.getOrDefault(channel, List.of()));
    }

    public List<Throwable> failures() {
        return List.copyOf(failures);
    }

    public void mode(BrokerMode value) {
        mode.set(value);
    }

    public void reset() {
        received.clear();
        failures.clear();
        mode.set(BrokerMode.ACKNOWLEDGE);
    }
}
