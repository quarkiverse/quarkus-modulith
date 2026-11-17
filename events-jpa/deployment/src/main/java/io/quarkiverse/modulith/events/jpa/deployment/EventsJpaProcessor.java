package io.quarkiverse.modulith.events.jpa.deployment;

import java.util.Set;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.jpa.AbstractJpaEventPublication;
import io.quarkiverse.modulith.events.jpa.ArchivedJpaEventPublication;
import io.quarkiverse.modulith.events.jpa.DefaultJpaEventPublicationRepository;
import io.quarkiverse.modulith.events.jpa.DefaultJpaReadOnlyTransactions;
import io.quarkiverse.modulith.events.jpa.EventsJpaConfig;
import io.quarkiverse.modulith.events.jpa.JpaEventPublication;
import io.quarkiverse.modulith.events.jpa.JpaSessions;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;

class EventsJpaProcessor {

    private static final String FEATURE = "modulith-events-jpa";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Adds the publication entity to the configured persistence unit, and the archive entity in {@code archive}
     * completion mode only, so that other modes need no archive table.
     */
    @BuildStep
    void entities(EventsJpaConfig jpa, EventsFixedConfig events, BuildProducer<AdditionalJpaModelBuildItem> models) {
        Set<String> units = Set.of(jpa.persistenceUnit());
        models.produce(new AdditionalJpaModelBuildItem(AbstractJpaEventPublication.class.getName(), units));
        models.produce(new AdditionalJpaModelBuildItem(JpaEventPublication.class.getName(), units));
        if (events.completionMode() == CompletionMode.ARCHIVE) {
            models.produce(new AdditionalJpaModelBuildItem(ArchivedJpaEventPublication.class.getName(), units));
        }
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultJpaEventPublicationRepository.class, JpaSessions.class,
                        DefaultJpaReadOnlyTransactions.class)
                .build();
    }
}
