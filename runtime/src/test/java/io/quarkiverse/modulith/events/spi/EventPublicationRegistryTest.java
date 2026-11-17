package io.quarkiverse.modulith.events.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventPublicationRegistryTest {

    @Test
    void claimsEveryPublicationByDefault() {
        // arrange
        EventPublicationRegistry registry = new MinimalRegistry();

        // act
        boolean claimed = registry.markProcessing(UUID.randomUUID());

        // assert
        assertThat(claimed).isTrue();
    }
}
