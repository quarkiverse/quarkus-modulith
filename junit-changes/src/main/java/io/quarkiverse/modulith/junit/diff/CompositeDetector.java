package io.quarkiverse.modulith.junit.diff;

import java.util.List;
import java.util.stream.Stream;

/**
 * Reports the files of all delegates, the default strategy combining uncommitted and unpushed changes.
 *
 * @param delegates the detectors to combine
 */
record CompositeDetector(List<FileModificationDetector> delegates) implements FileModificationDetector {

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return delegates.stream().flatMap(FileModificationDetector::getModifiedFiles);
    }
}
