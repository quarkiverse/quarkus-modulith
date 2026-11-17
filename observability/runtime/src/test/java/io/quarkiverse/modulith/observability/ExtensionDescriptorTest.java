package io.quarkiverse.modulith.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class ExtensionDescriptorTest {

    @Test
    void declaresNameStatusAndConfigPrefix() throws IOException {
        // arrange
        InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/META-INF/quarkus-extension.yaml"));

        // act
        List<String> lines;
        try (in) {
            lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }

        // assert
        assertThat(lines).startsWith(
                "---",
                "name: \"Modulith Observability\"",
                "description: \"OpenTelemetry spans and Micrometer metrics for module invocations and\\",
                "  \\ module events\"",
                "metadata:",
                "  keywords:",
                "  - \"modulith\"",
                "  - \"opentelemetry\"",
                "  - \"micrometer\"",
                "  guide: \"https://docs.quarkiverse.io/quarkus-modulith/dev/\"",
                "  categories:",
                "  - \"miscellaneous\"",
                "  status: \"experimental\"",
                "  unlisted: true",
                "  config:",
                "  - \"quarkus.modulith.observability.\"");
    }
}
