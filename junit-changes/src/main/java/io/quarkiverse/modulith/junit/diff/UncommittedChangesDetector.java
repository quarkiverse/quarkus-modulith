package io.quarkiverse.modulith.junit.diff;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;

/**
 * Reports the uncommitted changes of the working tree: staged, modified, removed, missing, conflicting and untracked
 * files.
 *
 * @param workingDirectory the directory the Git repository is looked up from
 */
record UncommittedChangesDetector(Path workingDirectory) implements FileModificationDetector {

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return JGitUtil.withRepository(workingDirectory, repository -> {
            try (var git = new Git(repository)) {
                var status = git.status().call();
                return Stream.concat(status.getUncommittedChanges().stream(), status.getUntracked().stream())
                        .map(ModifiedFile::new)
                        .toList()
                        .stream();
            }
        });
    }
}
