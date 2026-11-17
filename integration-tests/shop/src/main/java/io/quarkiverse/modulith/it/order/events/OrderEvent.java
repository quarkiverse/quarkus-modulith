package io.quarkiverse.modulith.it.order.events;

/**
 * Events about an order after it was placed. Listeners listen to this interface and receive its implementations, so
 * the event publication registry stores and reads back a subtype of the listened type.
 */
public interface OrderEvent {

    String orderId();
}
