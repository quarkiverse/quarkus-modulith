package io.quarkiverse.modulith.events.jpa;

import org.hibernate.query.MutationQuery;

/**
 * Runs the update and delete statements of the repository. Hibernate ORM 8 deprecates {@code executeUpdate()} for
 * {@code execute()}.
 */
final class Mutations {

    private Mutations() {
    }

    /**
     * @return the number of rows the statement changed
     */
    static int execute(MutationQuery query) {
        return query.execute();
    }
}
