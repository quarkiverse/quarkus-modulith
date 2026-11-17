package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jpa.deployment.replaced.OwnStore;
import io.quarkiverse.modulith.events.spi.ReadOnlyTransactions;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;
import io.quarkus.test.QuarkusExtensionTest;

public class ReplacedStoreTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(OwnStore.class))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    EventPublicationRepository repository;

    @Inject
    ReadOnlyTransactions readOnlyTransactions;

    @Test
    void usesTheStoreOfTheApplicationInsteadOfTheJpaOne() {
        // act
        Class<?> store = repository.getClass();

        // assert
        assertThat(store).isEqualTo(InMemoryEventPublicationRepository.class);
        assertThat(readOnlyTransactions).isSameAs(OwnStore.NO_READ_ONLY);
    }
}
