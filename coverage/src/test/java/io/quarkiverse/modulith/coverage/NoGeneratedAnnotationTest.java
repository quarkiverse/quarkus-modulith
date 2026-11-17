package io.quarkiverse.modulith.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * JaCoCo skips classes and methods annotated with {@code @Generated}. Hand-written code must never use it to escape
 * the coverage gate.
 */
class NoGeneratedAnnotationTest {

    private static final Pattern GENERATED = Pattern.compile("@(\\w+\\.)*Generated\\b");

    @Test
    void handWrittenSourcesAreNotMarkedAsGenerated() throws IOException {
        // arrange
        Path root = Path.of(System.getProperty("coverage.root", ".."));

        // act
        List<Path> offenders = MainSourceFiles.under(root).stream()
                .filter(NoGeneratedAnnotationTest::marksGenerated)
                .toList();

        // assert
        assertThat(offenders).isEmpty();
    }

    private static boolean marksGenerated(Path file) {
        try {
            return GENERATED.matcher(Files.readString(file)).find();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
