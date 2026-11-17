package io.quarkiverse.modulith.events.jpa.deployment.readonly;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;

public class ReadOnlyListener {

    public static final List<String> SEEN = new CopyOnWriteArrayList<>();

    @Inject
    Session session;

    @ApplicationModuleListener(id = "read-only", readOnlyTransaction = true)
    void on(OrderPlaced event) {
        SEEN.add("read-only=" + session.isDefaultReadOnly() + ",manual="
                + (session.getHibernateFlushMode() == FlushMode.MANUAL));
    }
}
