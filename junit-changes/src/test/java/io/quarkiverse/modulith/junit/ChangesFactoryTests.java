package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.junit.diff.GitRepositories;
import io.quarkiverse.modulith.junit.diff.TestConfigs;

class ChangesFactoryTests {

    @TempDir
    Path repository;

    @BeforeEach
    void createRepository() throws Exception {
        try (var git = GitRepositories.init(repository)) {
            GitRepositories.write(repository, "app/README.md", "1");
            GitRepositories.commitAll(git, "first");
        }
        GitRepositories.write(repository, "app/README.md", "2");
        GitRepositories.write(repository, "app/src/main/java/com/acme/Foo.java", "2");
        GitRepositories.write(repository, "other/src/main/java/com/acme/Bar.java", "2");
    }

    @Test
    void returnsNoChangesIfDisabledByProperty() {
        // arrange
        var config = TestConfigs.of(Map.of("quarkus.modulith.test.skip-optimizations", "true",
                "quarkus.modulith.test.on-no-changes", "invalid"));

        // act
        var result = ChangesFactory.getChanges(config, repository.resolve("app"));

        // assert
        assertThat(result).isSameAs(Changes.NONE);
        assertThat(result.skipTestsOnNoChanges()).isFalse();
    }

    @Test
    void detectsChangesOfWorkingDirectoryIfOptimizationsEnabled() {
        // arrange
        var config = TestConfigs.of(Map.of("quarkus.modulith.test.skip-optimizations", "false"));

        // act
        var result = ChangesFactory.getChanges(config, repository.resolve("app"));

        // assert
        assertThat(result).containsExactlyInAnyOrder(new OtherFileChange("README.md"),
                new JavaSourceChange("com.acme.Foo"));
    }

    @ParameterizedTest
    @CsvSource({ "skip-all, true", "execute-none, true", "execute-all, false" })
    void appliesOnNoChangesProperty(String value, boolean skip) {
        // arrange
        var config = TestConfigs.of(Map.of("quarkus.modulith.test.on-no-changes", value));

        // act
        var result = ChangesFactory.getChanges(config, repository.resolve("app"));

        // assert
        assertThat(result.skipTestsOnNoChanges()).isEqualTo(skip);
    }

    @Test
    void executesTestsOnNoChangesByDefault() {
        // arrange
        var config = TestConfigs.of(Map.of());

        // act
        var result = ChangesFactory.getChanges(config, repository.resolve("app"));

        // assert
        assertThat(result.skipTestsOnNoChanges()).isFalse();
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    void executesAllTestsWhenTheDetectionFails() {
        // arrange
        var config = TestConfigs.of(Map.of("quarkus.modulith.test.file-modification-detector", "reference-commit",
                "quarkus.modulith.test.reference-commit", "unknown-revision",
                "quarkus.modulith.test.on-no-changes", "skip-all"));

        // act
        var result = ChangesFactory.getChanges(config, repository.resolve("app"));

        // assert
        assertThat(result).isSameAs(Changes.NONE);
    }

    @Test
    void rejectsAnInvalidCustomDetector() {
        // arrange
        var config = TestConfigs.of(Map.of("quarkus.modulith.test.file-modification-detector", "java.lang.String"));

        // act
        var thrown = catchThrowable(() -> ChangesFactory.getChanges(config, repository.resolve("app")));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("java.lang.String does not implement "
                        + "io.quarkiverse.modulith.junit.diff.FileModificationDetector!");
    }

    @Test
    void rejectsInvalidOnNoChangesConfigurationValue() {
        // arrange
        var config = TestConfigs.of(Map.of("quarkus.modulith.test.on-no-changes", "1"));

        // act
        var thrown = catchThrowable(() -> ChangesFactory.getChanges(config, repository.resolve("app")));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid on-no-changes value '1'! Needs to be any of [execute-all, skip-all, execute-none]!");
    }
}
