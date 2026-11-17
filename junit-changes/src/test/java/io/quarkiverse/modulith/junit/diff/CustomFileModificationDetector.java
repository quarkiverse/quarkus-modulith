package io.quarkiverse.modulith.junit.diff;

import java.util.stream.Stream;

/**
 * A detector selected by its class name in the tests.
 */
record CustomFileModificationDetector() implements FileModificationDetector {

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return ModifiedFile.of("custom.txt");
    }
}
