package io.quarkiverse.modulith.junit.diff;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.BranchConfig;

/**
 * Reports the files changed between the local branch and its merge base with the tracking branch, that is the changes
 * of commits not pushed yet. Commits that only the tracking branch has (fetched but not pulled) do not count, unlike in
 * Spring Modulith, which compares with the tip of the tracking branch. It does not fetch from the remote first. Without
 * a tracking branch, for example on a detached {@code HEAD}, it reports nothing.
 *
 * @param workingDirectory the directory the Git repository is looked up from
 */
record UnpushedCommitsDetector(Path workingDirectory) implements FileModificationDetector {

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return JGitUtil.withRepository(workingDirectory, repository -> {
            var localBranch = repository.getFullBranch();
            var trackingBranch = new BranchConfig(repository.getConfig(), repository.getBranch()).getTrackingBranch();
            return Objects.nonNull(trackingBranch)
                    ? JGitUtil.toModifiedFiles(repository, JGitUtil.mergeBase(repository, localBranch, trackingBranch),
                            localBranch)
                    : Stream.<ModifiedFile> empty();
        });
    }
}
