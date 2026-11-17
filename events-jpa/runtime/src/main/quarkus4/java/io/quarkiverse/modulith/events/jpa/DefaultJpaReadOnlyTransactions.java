package io.quarkiverse.modulith.events.jpa;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

/**
 * Read-only JPA transactions as a reserve bean, replaced by any other
 * {@link io.quarkiverse.modulith.events.spi.ReadOnlyTransactions} bean. Used with Quarkus 4.
 */
@Singleton
@Reserve
@Priority(0)
public class DefaultJpaReadOnlyTransactions extends JpaReadOnlyTransactions {
}
