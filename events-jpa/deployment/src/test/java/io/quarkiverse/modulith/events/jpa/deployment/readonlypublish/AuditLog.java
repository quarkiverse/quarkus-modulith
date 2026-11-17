package io.quarkiverse.modulith.events.jpa.deployment.readonlypublish;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.ApplicationModuleListener;

@Singleton
public class AuditLog {

    public static final List<String> AUDITED = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(id = "audit-log")
    void on(OrderAudited event) {
        AUDITED.add(event.orderId());
    }
}
