package io.quarkiverse.modulith.it.springcompat.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the notification module.
 */
@ApplicationScoped
public class NotificationService {

    @Inject
    Clock clock;

    public String name() {
        return "notification";
    }
}
