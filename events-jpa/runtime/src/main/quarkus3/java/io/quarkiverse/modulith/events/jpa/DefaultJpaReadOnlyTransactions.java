package io.quarkiverse.modulith.events.jpa;

import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;

/**
 * Read-only JPA transactions as a default bean, replaced by any other
 * {@link io.quarkiverse.modulith.events.spi.ReadOnlyTransactions} bean. Used with Quarkus 3.
 */
@Singleton
@DefaultBean
public class DefaultJpaReadOnlyTransactions extends JpaReadOnlyTransactions {
}
