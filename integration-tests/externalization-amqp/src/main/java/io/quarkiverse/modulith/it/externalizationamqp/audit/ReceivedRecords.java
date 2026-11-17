package io.quarkiverse.modulith.it.externalizationamqp.audit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;

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

    private CompletionStage<Void> receive(Message<Object> message) {
        IncomingAmqpMetadata metadata = message.getMetadata(IncomingAmqpMetadata.class).orElseThrow();
        Map<String, String> properties = new TreeMap<>();
        Optional.ofNullable(metadata.getProperties()).ifPresent(json -> json.forEach(
                property -> properties.put(property.getKey(), String.valueOf(property.getValue()))));
        records.add(new ReceivedRecord(metadata.getAddress(), metadata.getSubject(), properties,
                metadata.getContentType(), String.valueOf(message.getPayload())));
        return message.ack();
    }

    public List<ReceivedRecord> records() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
