package io.quarkiverse.modulith.junit.diff;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;

/**
 * SPI to plug in the strategy that finds the files currently modified in a project, like Spring Modulith's
 * {@code FileModificationDetector}.
 * <p>
 * Select the strategy with {@value #DETECTOR_PROPERTY}: {@code uncommitted-changes}, {@code reference-commit},
 * {@code default}, or the fully qualified name of an implementation of this interface with a no-argument constructor.
 */
public interface FileModificationDetector {

    /**
     * The prefix of the configuration properties of change-aware test execution.
     */
    String CONFIG_PROPERTY_PREFIX = "quarkus.modulith.test";

    /**
     * Selects the detection strategy.
     */
    String DETECTOR_PROPERTY = CONFIG_PROPERTY_PREFIX + ".file-modification-detector";

    /**
     * The commit, branch or other revision to compare {@code HEAD} with, usually the commit of the last successful CI
     * build. The environment variable is {@code QUARKUS_MODULITH_TEST_REFERENCE_COMMIT}.
     */
    String REFERENCE_COMMIT_PROPERTY = CONFIG_PROPERTY_PREFIX + ".reference-commit";

    /**
     * Returns all {@link ModifiedFile}s detected.
     *
     * @return will never be {@code null}
     */
    Stream<ModifiedFile> getModifiedFiles();

    /**
     * Returns the configured {@link FileModificationDetector} for the current working directory.
     *
     * @param config the configuration to read {@value #DETECTOR_PROPERTY} and {@value #REFERENCE_COMMIT_PROPERTY}
     *        from, must not be {@code null}
     * @return will never be {@code null}
     * @see #getDetector(Config, Path)
     */
    static FileModificationDetector getDetector(Config config) {
        return getDetector(config, Path.of("").toAbsolutePath());
    }

    /**
     * Returns the configured {@link FileModificationDetector} for the given working directory. Only files located in
     * that directory are reported, with paths relative to it. Outside of a Git repository no file is reported.
     *
     * @param config the configuration, must not be {@code null}
     * @param workingDirectory the directory of the project under test, must not be {@code null}
     * @return will never be {@code null}
     */
    static FileModificationDetector getDetector(Config config, Path workingDirectory) {
        var target = getTargetDetector(config, workingDirectory);
        return WorkingDirectoryChangesDetector.of(target, workingDirectory)
                .orElseGet(() -> {
                    System.getLogger(FileModificationDetector.class.getName()).log(Level.WARNING,
                            "No Git repository found at or above {0}, considering no file changed.", workingDirectory);
                    return new CompositeDetector(List.of());
                });
    }

    /**
     * Returns the {@link FileModificationDetector} selected by {@value #DETECTOR_PROPERTY}, without restricting its
     * results to the working directory.
     *
     * @param config the configuration, must not be {@code null}
     * @param workingDirectory the directory the Git repository is looked up from, must not be {@code null}
     * @return will never be {@code null}
     * @throws IllegalStateException if a custom detector class cannot be loaded or instantiated
     * @throws IllegalArgumentException if a custom detector class does not implement this interface
     */
    static FileModificationDetector getTargetDetector(Config config, Path workingDirectory) {
        var referenceCommit = config.getOptionalValue(REFERENCE_COMMIT_PROPERTY, String.class);
        var selector = config.getOptionalValue(DETECTOR_PROPERTY, String.class)
                .filter(Predicate.not(String::isBlank))
                .orElse("default");
        return switch (selector) {
            case "uncommitted-changes" -> new UncommittedChangesDetector(workingDirectory);
            case "reference-commit" -> ReferenceCommitDetector.of(referenceCommit, workingDirectory);
            case "default" -> getDefaultDetector(referenceCommit, workingDirectory);
            default -> CustomDetectors.instantiate(selector);
        };
    }

    private static FileModificationDetector getDefaultDetector(Optional<String> referenceCommit, Path workingDirectory) {
        return referenceCommit.filter(Predicate.not(String::isBlank))
                .<FileModificationDetector> map(it -> ReferenceCommitDetector.of(referenceCommit, workingDirectory))
                .orElseGet(() -> {
                    System.getLogger(FileModificationDetector.class.getName()).log(Level.INFO,
                            "Using default file modification detector (uncommitted and unpushed changes).");
                    return new CompositeDetector(List.of(new UncommittedChangesDetector(workingDirectory),
                            new UnpushedCommitsDetector(workingDirectory)));
                });
    }
}
