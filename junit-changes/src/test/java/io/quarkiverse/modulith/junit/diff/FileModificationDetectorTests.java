package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileModificationDetectorTests {

    private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    @TempDir
    Path temp;

    @AfterEach
    void restoreContextClassLoader() {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Test
    void usesReferenceCommitDetectionIfHashConfigured() {
        // arrange
        var config = config(null, "HEAD^");

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new ReferenceCommitDetector("HEAD^", temp));
    }

    @Test
    void usesReferenceCommitDetectionIfConfiguredExplicitly() {
        // arrange
        var config = config("reference-commit", null);

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new ReferenceCommitDetector("HEAD", temp));
    }

    @Test
    void usesConfiguredReferenceCommitIfReferenceCommitDetectionSelected() {
        // arrange
        var config = config("reference-commit", "abc123");

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new ReferenceCommitDetector("abc123", temp));
    }

    @Test
    void usesUncommittedChangesIfConfiguredExplicitly() {
        // arrange
        var config = config("uncommitted-changes", "abc123");

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new UncommittedChangesDetector(temp));
    }

    @Test
    void registersCustomDetectorByType() {
        // arrange
        var config = config(CustomFileModificationDetector.class.getName(), null);

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new CustomFileModificationDetector());
    }

    @Test
    void loadsCustomDetectorWithTheContextClassLoader() {
        // arrange
        var config = config(CustomFileModificationDetector.class.getName(), null);
        Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());

        // act
        var thrown = catchThrowable(() -> FileModificationDetector.getTargetDetector(config, temp));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to instantiate file modification detector "
                        + CustomFileModificationDetector.class.getName())
                .hasCauseExactlyInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void loadsCustomDetectorWithTheSpiClassLoaderWithoutContextClassLoader() {
        // arrange
        var config = config(CustomFileModificationDetector.class.getName(), null);
        Thread.currentThread().setContextClassLoader(null);

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new CustomFileModificationDetector());
    }

    @ParameterizedTest
    @ValueSource(strings = { "default", " " })
    void selectingDefaultExplicitlyUsesDefault(String selector) {
        // arrange
        var config = config(selector, null);

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(FileModificationDetector.getTargetDetector(config(null, null), temp))
                .isEqualTo(new CompositeDetector(
                        List.of(new UncommittedChangesDetector(temp), new UnpushedCommitsDetector(temp))));
    }

    @Test
    void defaultDetectorUsesConfiguredReferenceCommit() {
        // arrange
        var config = config("default", "main~2");

        // act
        var detector = FileModificationDetector.getTargetDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new ReferenceCommitDetector("main~2", temp));
    }

    @Test
    void defaultDetectorCombinesUncommittedAndUnpushedChanges() throws Exception {
        // arrange
        var remote = temp.resolve("remote");
        var local = temp.resolve("local");
        try (var git = GitRepositories.init(remote)) {
            GitRepositories.write(remote, "pushed.txt", "1");
            GitRepositories.commitAll(git, "first");
        }
        try (var git = GitRepositories.cloneOf(remote, local)) {
            GitRepositories.write(local, "unpushed.txt", "2");
            GitRepositories.commitAll(git, "second");
        }
        GitRepositories.write(local, "uncommitted.txt", "3");

        // act
        var result = FileModificationDetector.getDetector(config(null, null), local).getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path).containsExactlyInAnyOrder("uncommitted.txt", "unpushed.txt");
    }

    @Test
    void rejectsInvalidDetectorName() {
        // arrange
        var config = config("some.Garbage", null);

        // act
        var thrown = catchThrowable(() -> FileModificationDetector.getDetector(config, temp));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to instantiate file modification detector some.Garbage")
                .hasCauseExactlyInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void rejectsDetectorWithoutDefaultConstructor() {
        // arrange
        var config = config(DetectorWithoutDefaultConstructor.class.getName(), null);

        // act
        var thrown = catchThrowable(() -> FileModificationDetector.getTargetDetector(config, temp));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to instantiate file modification detector "
                        + DetectorWithoutDefaultConstructor.class.getName())
                .hasCauseExactlyInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void rejectsTypeNotImplementingDetector() {
        // arrange
        var config = config("java.lang.String", null);

        // act
        var thrown = catchThrowable(() -> FileModificationDetector.getTargetDetector(config, temp));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("java.lang.String does not implement "
                        + "io.quarkiverse.modulith.junit.diff.FileModificationDetector!");
    }

    @Test
    void restrictsDetectorToWorkingDirectory() throws Exception {
        // arrange
        try (var git = GitRepositories.init(temp)) {
            GitRepositories.write(temp, "module/pom.xml", "1");
            GitRepositories.commitAll(git, "first");
        }
        var module = temp.resolve("module");

        // act
        var detector = FileModificationDetector.getDetector(config("uncommitted-changes", null), module);

        // assert
        assertThat(detector)
                .isEqualTo(new WorkingDirectoryChangesDetector(new UncommittedChangesDetector(module), "module"));
    }

    @Test
    void detectsNoFilesOutsideOfGitRepository() {
        // arrange
        var config = config(CustomFileModificationDetector.class.getName(), null);

        // act
        var detector = FileModificationDetector.getDetector(config, temp);

        // assert
        assertThat(detector).isEqualTo(new CompositeDetector(List.of()));
        assertThat(detector.getModifiedFiles()).isEmpty();
    }

    @Test
    void usesCurrentWorkingDirectoryByDefault() {
        // arrange
        var config = config(CustomFileModificationDetector.class.getName(), null);

        // act
        var detector = FileModificationDetector.getDetector(config);

        // assert
        assertThat(detector).isEqualTo(FileModificationDetector.getDetector(config, Path.of("").toAbsolutePath()));
    }

    private static Config config(String detector, String referenceCommit) {
        var properties = new HashMap<String, String>();
        if (Objects.nonNull(detector)) {
            properties.put("quarkus.modulith.test.file-modification-detector", detector);
        }
        if (Objects.nonNull(referenceCommit)) {
            properties.put("quarkus.modulith.test.reference-commit", referenceCommit);
        }
        return TestConfigs.of(Map.copyOf(properties));
    }
}
