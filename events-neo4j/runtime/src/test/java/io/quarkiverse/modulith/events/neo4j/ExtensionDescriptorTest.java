package io.quarkiverse.modulith.events.neo4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class ExtensionDescriptorTest {

    private static final String DESCRIPTOR = "/META-INF/quarkus-extension.yaml";

    @Test
    void declaresNameStatusAndConfigPrefix() throws IOException {
        // act
        String yaml = read(DESCRIPTOR);

        // assert
        assertThat(yaml).contains("Modulith Neo4j", "experimental", "quarkus.modulith.events.neo4j.");
    }

    private String read(String resource) throws IOException {
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream(resource), resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
