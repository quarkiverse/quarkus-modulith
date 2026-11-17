package io.quarkiverse.modulith.junit;

import java.lang.System.Logger.Level;
import java.nio.file.Path;

import org.eclipse.microprofile.config.Config;

import io.quarkiverse.modulith.junit.diff.FileModificationDetector;

/**
 * Creates the {@link Changes} of a project from the configuration.
 */
final class ChangesFactory {

    static final String SKIP_OPTIMIZATIONS_PROPERTY = FileModificationDetector.CONFIG_PROPERTY_PREFIX
            + ".skip-optimizations";

    static final String ON_NO_CHANGES_PROPERTY = FileModificationDetector.CONFIG_PROPERTY_PREFIX + ".on-no-changes";

    private static final System.Logger LOG = System.getLogger(ChangesFactory.class.getName());

    private ChangesFactory() {
    }

    /**
     * @param config the configuration, must not be {@code null}
     * @param workingDirectory the directory of the project under test
     * @return {@link Changes#NONE} if {@code quarkus.modulith.test.skip-optimizations} is {@code true} or the detector
     *         fails to read the changes (for example an unknown reference commit or a deleted tracking branch),
     *         otherwise the changes found by the configured detector
     * @throws IllegalArgumentException for an invalid {@code quarkus.modulith.test.on-no-changes} value or a custom
     *         detector class that does not implement {@link FileModificationDetector}
     * @throws IllegalStateException if a custom detector class cannot be loaded or instantiated
     */
    static Changes getChanges(Config config, Path workingDirectory) {
        if (config.getOptionalValue(SKIP_OPTIMIZATIONS_PROPERTY, Boolean.class).orElse(false)) {
            return Changes.NONE;
        }
        var onNoChanges = OnNoChange.fromConfig(config.getOptionalValue(ON_NO_CHANGES_PROPERTY, String.class));
        var detector = FileModificationDetector.getDetector(config, workingDirectory);
        return detect(detector, onNoChanges);
    }

    private static Changes detect(FileModificationDetector detector, OnNoChange onNoChanges) {
        try {
            var result = Changes.of(detector.getModifiedFiles(), onNoChanges);
            LOG.log(Level.INFO, "Detected changes:\n{0}", result);
            return result;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Unable to detect the changed files, executing all tests.", e);
            return Changes.NONE;
        }
    }
}
