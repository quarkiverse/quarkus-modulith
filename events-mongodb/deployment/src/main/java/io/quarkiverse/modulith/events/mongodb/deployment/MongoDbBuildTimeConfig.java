package io.quarkiverse.modulith.events.mongodb.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time settings of the MongoDB event publication registry.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.mongodb")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface MongoDbBuildTimeConfig {

    /**
     * Transaction management, the counterpart of `spring.modulith.events.mongodb.transaction-management.enabled`.
     */
    TransactionManagement transactionManagement();

    /**
     * Transaction management.
     */
    interface TransactionManagement {

        /**
         * Whether publications are written in the MongoDB transaction of the business transaction: the client session
         * MongoDB with Panache binds to a `@Transactional` method. Needs `quarkus-mongodb-panache` and a replica set.
         * Without Panache every publication is written on its own. Takes effect at build time.
         */
        @WithDefault("true")
        boolean enabled();
    }
}
