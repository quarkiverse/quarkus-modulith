package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class NoListenersProdModeTest {

    static final String CYCLE = "io.quarkiverse.modulith.deployment.fixtures.cycle";

    @RegisterExtension
    static final QuarkusProdModeTest prod = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addPackages(true, CYCLE))
            .setApplicationName("no-listeners")
            .setApplicationVersion("1.0")
            .overrideConfigKey("quarkus.modulith.base-package", CYCLE)
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn");

    @ProdBuildResults
    ProdModeTestResults results;

    @Test
    void leavesTheDispatcherOut() throws IOException {
        // arrange
        Path generated = results.getBuildDir().resolve("quarkus-app/quarkus/generated-bytecode.jar");

        // act
        List<String> classes;
        try (ZipFile jar = new ZipFile(generated.toFile())) {
            classes = jar.stream().map(ZipEntry::getName).toList();
        }

        // assert
        assertThat(classes).isNotEmpty().noneMatch(it -> it.contains("ModuleEventDispatcher"));
    }

    @Test
    void writesTheDocumentation() {
        // act
        Path components = results.getBuildDir().resolve("modulith-docs").resolve("components.puml");

        // assert
        assertThat(components).exists();
    }
}
