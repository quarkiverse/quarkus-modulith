package io.quarkiverse.modulith.it.notification.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Notification {

    @Id
    public String orderId;

    public String text;
}
