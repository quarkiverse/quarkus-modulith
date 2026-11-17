package io.quarkiverse.modulith.events.jpa;

import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;

/**
 * The JPA store as a default bean, replaced by any other
 * {@link io.quarkiverse.modulith.events.core.EventPublicationRepository} bean. Used with Quarkus 3.
 */
@Singleton
@DefaultBean
public class DefaultJpaEventPublicationRepository extends JpaEventPublicationRepository {
}
