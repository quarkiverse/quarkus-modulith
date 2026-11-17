package io.quarkiverse.modulith.it.order.events;

import java.util.List;

/**
 * A subtype of the listened {@link OrderEvent}, with a nested record and a list, the shapes the native executable must
 * be able to serialize and read back.
 */
public record OrderShipped(String orderId, Address address, List<String> parcels) implements OrderEvent {
}
