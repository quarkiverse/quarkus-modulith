package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfig;

class ModulithTestConfigTests {

    @TempDir
    Path classes;

    @Test
    void readsApplicationPropertiesWithTestProfile() throws Exception {
        // arrange
        Files.writeString(classes.resolve("application.properties"), """
                modulith.config-test.plain=plain
                modulith.config-test.profiled=default
                %test.modulith.config-test.profiled=test
                %prod.modulith.config-test.prod-only=prod
                """);

        try (var loader = new URLClassLoader(new URL[] { classes.toUri().toURL() }, null)) {
            // act
            var config = ModulithTestConfig.load(loader);

            // assert
            assertThat(config.getOptionalValue("modulith.config-test.plain", String.class)).contains("plain");
            assertThat(config.getOptionalValue("modulith.config-test.profiled", String.class)).contains("test");
            assertThat(config.getOptionalValue("modulith.config-test.prod-only", String.class))
                    .isEqualTo(Optional.empty());
        }
    }

    @Test
    void loadsWithContextClassLoader() {
        // arrange
        var expectedProfiles = List.of("test");

        // act
        var config = ModulithTestConfig.load();

        // assert
        assertThat(config.unwrap(SmallRyeConfig.class).getProfiles()).isEqualTo(expectedProfiles);
    }
}
