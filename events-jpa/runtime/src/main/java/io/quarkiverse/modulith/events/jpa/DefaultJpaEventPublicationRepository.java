package io.quarkiverse.modulith.events.jpa;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

/**
 * The JPA store as a reserve bean, replaced by any other
 * {@link io.quarkiverse.modulith.events.core.EventPublicationRepository} bean.
 */
@Singleton
@Reserve
@Priority(0)
public class DefaultJpaEventPublicationRepository extends JpaEventPublicationRepository {
}
