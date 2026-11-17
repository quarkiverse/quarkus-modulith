package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.OutgoingMessageMetadata;

/**
 * A broker that stops requesting messages: it requests one and leaves it unacknowledged until released, then takes
 * and acknowledges every message.
 */
@Singleton
public class ThrottledBroker {

    private final List<Message<?>> received = new ArrayList<>();
    private final List<Throwable> failures = new ArrayList<>();
    private Flow.Subscription subscription;
    private boolean released;

    synchronized void subscribe(Flow.Subscription value) {
        subscription = value;
        subscription.request(1);
    }

    synchronized void receive(Message<?> message) {
        received.add(message);
        if (released) {
            acknowledge(message);
        }
    }

    synchronized void fail(Throwable failure) {
        failures.add(failure);
    }

    public synchronized void release() {
        released = true;
        received.forEach(ThrottledBroker::acknowledge);
        subscription.request(Long.MAX_VALUE);
    }

    public synchronized List<Message<?>> received() {
        return List.copyOf(received);
    }

    public synchronized List<Throwable> failures() {
        return List.copyOf(failures);
    }

    private static void acknowledge(Message<?> message) {
        OutgoingMessageMetadata.setResultOnMessage(message, "stored");
        message.ack();
    }
}
