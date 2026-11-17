package io.quarkiverse.modulith.events.jpa.deployment.tck;

import java.util.List;

import jakarta.persistence.EntityManager;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Empties the publication tables between contract tests.
 */
public final class Tables {

    private Tables() {
    }

    public static void clear(EntityManager entityManager, List<String> entities) {
        QuarkusTransaction.requiringNew().run(() -> entities
                .forEach(entity -> Statements.deleteAll(entityManager, entity)));
    }
}
