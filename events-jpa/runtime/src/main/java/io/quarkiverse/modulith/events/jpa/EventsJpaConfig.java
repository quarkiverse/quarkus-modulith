package io.quarkiverse.modulith.events.jpa;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Settings of the Hibernate ORM store of the event publication registry.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.jpa")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface EventsJpaConfig {

    /**
     * The persistence unit holding the event publication tables. The publishing transaction must use the same
     * datasource for the outbox guarantee to hold.
     */
    @WithDefault(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
    String persistenceUnit();
}
