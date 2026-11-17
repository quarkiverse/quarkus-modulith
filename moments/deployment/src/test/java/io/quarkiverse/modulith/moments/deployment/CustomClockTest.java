package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.deployment.fixtures.ClockProducer;
import io.quarkiverse.modulith.moments.support.Now;
import io.quarkus.test.QuarkusExtensionTest;

class CustomClockTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ClockProducer.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Now now;

    @Test
    void usesTheClockBean() {
        // act
        Instant instant = now.instant();

        // assert
        assertThat(instant).isEqualTo(Instant.parse("2026-11-16T10:15:30Z"));
        assertThat(now.now()).isEqualTo(LocalDateTime.of(2026, 11, 16, 10, 15, 30));
    }
}
