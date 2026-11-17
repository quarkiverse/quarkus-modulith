package io.quarkiverse.modulith.events.jpa.deployment.tck;

import jakarta.persistence.EntityManager;

/**
 * Runs the delete statements of the tests. Quarkus 4 variant: Jakarta Persistence 4 runs statements through {@code createStatement}.
 */
public final class Statements {

    private Statements() {
    }

    public static void deleteAll(EntityManager entityManager, String entity) {
        entityManager.createStatement("delete from " + entity).execute();
    }
}
