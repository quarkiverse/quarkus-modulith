package io.quarkiverse.modulith.deployment.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Packaging writes the documentation of the fixture application with the default options, using the Javadoc of the
 * annotation processor.
 */
public class DocsProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest prod = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addPackages(true, DocsFixture.BASE)
                    .addAsResource("docs/javadoc.json", "META-INF/quarkus-modulith/javadoc.json"))
            .setApplicationName("docs")
            .setApplicationVersion("1.0")
            .overrideConfigKey("quarkus.modulith.docs.clean", "false");

    @ProdBuildResults
    ProdModeTestResults results;

    @Test
    void writesTheDocumentation() throws URISyntaxException {
        // arrange
        Path docs = results.getBuildDir().resolve("modulith-docs");
        Path golden = Path.of(DocsProdModeTest.class.getResource("/docs/golden/default").toURI());

        // act
        Map<String, String> files = read(docs);

        // assert
        assertThat(files).isEqualTo(read(golden));
    }

    static Map<String, String> read(Path folder) {
        try (Stream<Path> paths = Files.list(folder)) {
            Map<String, String> result = new TreeMap<>();
            for (Path file : paths.toList()) {
                result.put(file.getFileName().toString(), Files.readString(file));
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
