package io.quarkiverse.modulith.events.jpa;

import jakarta.inject.Inject;

import org.hibernate.FlushMode;

import io.quarkiverse.modulith.events.spi.ReadOnlyTransactions;

/**
 * Makes the Hibernate session of the current transaction read-only: entities are loaded read-only and nothing is
 * flushed, like Spring's read-only JPA transactions. The bean is {@code DefaultJpaReadOnlyTransactions}, a default bean
 * that an application's own {@link ReadOnlyTransactions} replaces.
 */
public class JpaReadOnlyTransactions implements ReadOnlyTransactions {

    @Inject
    JpaSessions sessions;

    @Override
    public void markReadOnly() {
        sessions.session().setDefaultReadOnly(true);
        sessions.session().setHibernateFlushMode(FlushMode.MANUAL);
    }
}
