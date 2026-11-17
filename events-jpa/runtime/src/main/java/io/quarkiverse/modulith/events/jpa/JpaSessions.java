package io.quarkiverse.modulith.events.jpa;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * The session of the persistence unit chosen with {@code quarkus.modulith.events.jpa.persistence-unit}, resolved once.
 * The session is the transaction-scoped proxy, so it always works on the current transaction.
 */
@Singleton
public class JpaSessions {

    @Inject
    @Any
    Instance<Session> sessions;

    @Inject
    EventsJpaConfig config;

    Session session;

    @PostConstruct
    void init() {
        session = PersistenceUnitUtil.isDefaultPersistenceUnit(config.persistenceUnit())
                ? sessions.select(Default.Literal.INSTANCE).get()
                : sessions.select(new PersistenceUnit.PersistenceUnitLiteral(config.persistenceUnit())).get();
    }

    Session session() {
        return session;
    }
}
