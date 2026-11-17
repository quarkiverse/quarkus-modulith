package io.quarkiverse.modulith.it.notification.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stands in for a mail provider that can be switched off to simulate an outage.
 */
@ApplicationScoped
public class MailGateway {

    private final AtomicBoolean down = new AtomicBoolean();

    public void setDown(boolean value) {
        down.set(value);
    }

    public void send(String text) {
        if (down.get()) {
            throw new IllegalStateException("Mail provider unavailable");
        }
    }
}
