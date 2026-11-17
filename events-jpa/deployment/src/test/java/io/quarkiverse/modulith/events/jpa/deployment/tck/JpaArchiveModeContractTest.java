package io.quarkiverse.modulith.events.jpa.deployment.tck;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.ArchiveCompletionModeContract;
import io.quarkus.test.QuarkusExtensionTest;

public class JpaArchiveModeContractTest extends ArchiveCompletionModeContract {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Tables.class, Statements.class, TransactionalRepository.class))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "archive");

    @Inject
    EventPublicationRepository jpa;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void clear() {
        Tables.clear(entityManager, List.of("JpaEventPublication", "ArchivedJpaEventPublication"));
    }

    @Override
    protected EventPublicationRepository repository() {
        return new TransactionalRepository(jpa);
    }
}
