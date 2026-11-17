package io.quarkiverse.modulith.junit.diff;

import java.util.stream.Stream;

/**
 * A detector that cannot be selected by its class name.
 */
record DetectorWithoutDefaultConstructor(String file) implements FileModificationDetector {

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return ModifiedFile.of(file);
    }
}
