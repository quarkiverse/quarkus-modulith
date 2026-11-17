package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.concurrent.Flow;

import org.eclipse.microprofile.reactive.messaging.Message;

public class ThrottledSubscriber implements Flow.Subscriber<Message<?>> {

    private final ThrottledBroker broker;

    ThrottledSubscriber(ThrottledBroker broker) {
        this.broker = broker;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        broker.subscribe(subscription);
    }

    @Override
    public void onNext(Message<?> message) {
        broker.receive(message);
    }

    @Override
    public void onError(Throwable failure) {
        broker.fail(failure);
    }

    @Override
    public void onComplete() {
        broker.fail(new IllegalStateException("channel completed"));
    }
}
