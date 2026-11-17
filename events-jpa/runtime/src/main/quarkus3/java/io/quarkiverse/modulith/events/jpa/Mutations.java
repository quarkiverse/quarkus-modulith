package io.quarkiverse.modulith.events.jpa;

import org.hibernate.query.MutationQuery;

/**
 * Runs the update and delete statements of the repository. Quarkus 3 variant: Hibernate ORM 7 runs a mutation query with {@code executeUpdate()}.
 */
final class Mutations {

    private Mutations() {
    }

    /**
     * @return the number of rows the statement changed
     */
    static int execute(MutationQuery query) {
        return query.executeUpdate();
    }
}
