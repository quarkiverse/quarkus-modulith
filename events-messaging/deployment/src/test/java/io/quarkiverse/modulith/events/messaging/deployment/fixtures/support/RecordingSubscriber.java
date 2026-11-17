package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.concurrent.Flow;

import org.eclipse.microprofile.reactive.messaging.Message;

public class RecordingSubscriber implements Flow.Subscriber<Message<?>> {

    private final String channel;
    private final RecordingBroker broker;

    RecordingSubscriber(String channel, RecordingBroker broker) {
        this.channel = channel;
        this.broker = broker;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Message<?> message) {
        broker.receive(channel, message);
    }

    @Override
    public void onError(Throwable failure) {
        broker.fail(failure);
    }

    @Override
    public void onComplete() {
        broker.complete(channel);
    }
}
