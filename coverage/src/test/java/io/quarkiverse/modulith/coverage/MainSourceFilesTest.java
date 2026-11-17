package io.quarkiverse.modulith.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainSourceFilesTest {

    private static final String MAIN_SOURCE = "core/src/main/java/Main.java";

    @TempDir
    Path root;

    @Test
    void findsOnlyJavaSourcesUnderSrcMainOutsideBuildOutput() throws IOException {
        // arrange
        Path main = write(MAIN_SOURCE);
        write("core/src/main/resources/application.properties");
        write("core/src/test/java/MainTest.java");
        write("core/target/src/main/java/Copied.java");
        write("node_modules/pkg/src/main/java/Vendored.java");
        write(".git/src/main/java/Object.java");

        // act
        List<Path> sources = MainSourceFiles.under(root);

        // assert
        assertThat(sources).containsExactly(main);
    }

    @Test
    void findsTheMainSourcesOfThisRepository() throws IOException {
        // arrange
        Path repository = Path.of(System.getProperty("coverage.root", ".."));

        // act
        List<Path> sources = MainSourceFiles.under(repository);

        // assert
        assertThat(sources).contains(repository.resolve(
                "core/src/main/java/io/quarkiverse/modulith/core/ApplicationModules.java"));
    }

    private Path write(String relative) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "");
    }
}
