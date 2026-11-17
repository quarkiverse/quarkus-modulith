package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ApplicationModuleTestProfileTest {

    @Test
    void takesTheSettingsOfTheAnnotation() {
        // act
        Map<String, String> config = new AnnotatedProfile().getConfigOverrides();

        // assert
        assertThat(config).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.modulith.test.module", "order",
                "quarkus.modulith.test.bootstrap-mode", "direct-dependencies",
                "quarkus.modulith.test.verify-automatically", "false",
                "quarkus.modulith.test.extra-modules", "billing,audit"));
    }

    @Test
    void modeWinsOverValueAndKeepsAdditionalConfig() {
        // act
        Map<String, String> config = new ModeProfile().getConfigOverrides();

        // assert
        assertThat(config).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.modulith.test.module", "io.quarkiverse.modulith.test",
                "quarkus.modulith.test.bootstrap-mode", "all-dependencies",
                "quarkus.modulith.test.verify-automatically", "true",
                "quarkus.log.level", "DEBUG"));
    }

    @Test
    void defaultsWithoutAnnotation() {
        // act
        Map<String, String> config = new PlainProfile().getConfigOverrides();

        // assert
        assertThat(config).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.modulith.test.module", "io.quarkiverse.modulith.test",
                "quarkus.modulith.test.bootstrap-mode", "standalone",
                "quarkus.modulith.test.verify-automatically", "true"));
    }

    @Test
    void ignoresAnUnannotatedDeclaringClass() {
        // arrange
        ApplicationModuleTestProfile profile = new ApplicationModuleTestProfile() {
        };

        // act
        Map<String, String> config = profile.getConfigOverrides();

        // assert
        assertThat(config).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.modulith.test.module", "io.quarkiverse.modulith.test",
                "quarkus.modulith.test.bootstrap-mode", "standalone",
                "quarkus.modulith.test.verify-automatically", "true"));
    }
}
