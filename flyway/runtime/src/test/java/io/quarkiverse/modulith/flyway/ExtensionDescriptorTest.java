package io.quarkiverse.modulith.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ExtensionDescriptorTest {

    @Test
    void declaresNameStatusAndConfigPrefix() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/META-INF/quarkus-extension.yaml")) {
            assertThat(in).isNotNull();
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(yaml).contains("Modulith Flyway", "experimental", "quarkus.modulith.flyway.");
        }
    }
}
