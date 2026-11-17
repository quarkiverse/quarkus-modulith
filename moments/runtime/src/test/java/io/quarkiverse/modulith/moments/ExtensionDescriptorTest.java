package io.quarkiverse.modulith.moments;

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
                "name: \"Modulith Moments\"",
                "description: \"Passage of time events (second, minute, hour, day, week, month, quarter,\\",
                "  \\ year) and a time machine for tests\"",
                "metadata:",
                "  keywords:",
                "  - \"modulith\"",
                "  - \"time\"",
                "  - \"scheduler\"",
                "  guide: \"https://docs.quarkiverse.io/quarkus-modulith/dev/\"",
                "  categories:",
                "  - \"miscellaneous\"",
                "  status: \"experimental\"",
                "  unlisted: true",
                "  config:",
                "  - \"quarkus.modulith.moments.\"");
    }
}
