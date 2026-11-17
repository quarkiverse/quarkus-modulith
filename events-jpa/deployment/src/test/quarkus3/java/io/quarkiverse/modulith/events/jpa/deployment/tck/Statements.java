package io.quarkiverse.modulith.events.jpa.deployment.tck;

import jakarta.persistence.EntityManager;

/**
 * Runs the delete statements of the tests. Quarkus 3 variant.
 */
public final class Statements {

    private Statements() {
    }

    public static void deleteAll(EntityManager entityManager, String entity) {
        entityManager.createQuery("delete from " + entity).executeUpdate();
    }
}
