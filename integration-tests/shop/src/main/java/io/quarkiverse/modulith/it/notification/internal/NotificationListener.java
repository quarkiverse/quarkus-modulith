package io.quarkiverse.modulith.it.notification.internal;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.it.order.events.OrderEvent;
import io.quarkiverse.modulith.it.order.events.OrderPlaced;
import io.quarkiverse.modulith.it.order.events.OrderShipped;

public class NotificationListener {

    @Inject
    EntityManager entityManager;

    @Inject
    MailGateway mail;

    @ApplicationModuleListener(id = "notification.order-confirmation")
    void on(OrderPlaced event) {
        String text = "Order " + event.orderId() + " placed";
        mail.send(text);
        Notification notification = new Notification();
        notification.orderId = event.orderId();
        notification.text = text;
        entityManager.persist(notification);
    }

    /**
     * Listens to the interface, so the stored publication carries the concrete event type.
     */
    @ApplicationModuleListener(id = "notification.shipment")
    void on(OrderEvent event) {
        if (event instanceof OrderShipped shipped) {
            String text = "Order " + shipped.orderId() + " shipped to " + shipped.address().city() + " in "
                    + shipped.parcels().size() + " parcels";
            mail.send(text);
            Notification notification = new Notification();
            notification.orderId = "shipped-" + shipped.orderId();
            notification.text = text;
            entityManager.persist(notification);
        }
    }
}
