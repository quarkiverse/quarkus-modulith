package io.quarkiverse.modulith.events.jdbc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseType;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcStore;
import io.quarkus.test.QuarkusExtensionTest;

public class JdbcRepositoryBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Shipped.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:bean;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.modulith.events.jdbc.schema-initialization.enabled", "true")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "delete");

    @Inject
    EventPublicationRepository repository;

    @Inject
    JdbcStore store;

    @Test
    void storesPublicationsInTheDefaultDatasource() {
        // arrange
        TargetEventPublication publication = TargetEventPublication.of(new Shipped("o-1"),
                PublicationTargetIdentifier.of("shipping.on-shipped"), Instant.parse("2026-01-01T10:00:00Z"));

        // act
        repository.create(publication);

        // assert
        assertThat(repository).isInstanceOf(JdbcEventPublicationRepository.class);
        assertThat(repository.findByStatus(Status.PUBLISHED)).singleElement().satisfies(p -> {
            assertThat(p.getEvent()).isEqualTo(new Shipped("o-1"));
            assertThat(p.getListenerId()).isEqualTo("shipping.on-shipped");
        });
        assertThat(store.settings().databaseType()).isEqualTo(DatabaseType.H2);
        assertThat(store.settings().completion()).isEqualTo(CompletionMode.DELETE);
        assertThat(store.settings().legacyStructure()).isFalse();
        assertThat(store.settings().schema()).isEmpty();
    }
}
