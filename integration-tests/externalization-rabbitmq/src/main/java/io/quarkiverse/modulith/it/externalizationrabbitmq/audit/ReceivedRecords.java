package io.quarkiverse.modulith.it.externalizationrabbitmq.audit;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;

@ApplicationScoped
public class ReceivedRecords {

    private final List<ReceivedRecord> records = new CopyOnWriteArrayList<>();

    @Incoming("orders-in")
    public CompletionStage<Void> orders(Message<Object> message) {
        return receive(message);
    }

    @Incoming("shipments-in")
    public CompletionStage<Void> shipments(Message<Object> message) {
        return receive(message);
    }

    @Incoming("eu-orders-in")
    public CompletionStage<Void> euOrders(Message<Object> message) {
        return receive(message);
    }

    @Incoming("fallback-in")
    public CompletionStage<Void> fallback(Message<Object> message) {
        return receive(message);
    }

    private CompletionStage<Void> receive(Message<Object> message) {
        IncomingRabbitMQMetadata metadata = message.getMetadata(IncomingRabbitMQMetadata.class).orElseThrow();
        Map<String, String> headers = new TreeMap<>();
        metadata.getHeaders().forEach((name, value) -> headers.put(name, String.valueOf(value)));
        Object payload = message.getPayload();
        String value = payload instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8)
                : String.valueOf(payload);
        records.add(new ReceivedRecord(metadata.getExchange(), metadata.getRoutingKey(), headers,
                ContentTypes.of(metadata), value));
        return message.ack();
    }

    public List<ReceivedRecord> records() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
