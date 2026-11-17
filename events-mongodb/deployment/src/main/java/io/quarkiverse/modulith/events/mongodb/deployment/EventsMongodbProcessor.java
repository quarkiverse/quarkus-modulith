package io.quarkiverse.modulith.events.mongodb.deployment;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStoreProducer;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.mongodb.runtime.PanacheMongoSessions;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Build steps of {@code quarkus-modulith-events-mongodb}: registers the repository, and decides whether publications
 * join the MongoDB transaction of MongoDB with Panache.
 */
class EventsMongodbProcessor {

    private static final Logger LOG = Logger.getLogger(EventsMongodbProcessor.class);

    static final String FEATURE = "modulith-events-mongodb";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans(MongoDbBuildTimeConfig config, Capabilities capabilities) {
        boolean panache = (capabilities.isPresent(Capability.MONGODB_PANACHE)
                || capabilities.isPresent(Capability.MONGODB_PANACHE_KOTLIN))
                && capabilities.isPresent(Capability.TRANSACTIONS);
        boolean transactional = config.transactionManagement().enabled() && panache;
        if (config.transactionManagement().enabled() && !panache) {
            LOG.warn("quarkus.modulith.events.mongodb.transaction-management.enabled is true, but MongoDB with Panache "
                    + "(Java or Kotlin) or JTA transactions are missing, so event publications are written outside the business "
                    + "transaction. Add quarkus-mongodb-panache, or set the property to false.");
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(MongoStoreProducer.class, MongoDbEventPublicationRepository.class,
                        transactional ? PanacheMongoSessions.class : NoMongoSessions.class)
                .build();
    }
}
