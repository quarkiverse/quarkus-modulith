package io.quarkiverse.modulith.junit.diff;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reports the files changed between a reference revision and the current branch: a complete or abbreviated commit
 * hash, a branch or another revision such as {@code HEAD~2}. See {@link org.eclipse.jgit.lib.Repository#resolve(String)}
 * for the accepted syntax. Only committed changes count.
 *
 * @param referenceCommit the revision to compare with
 * @param workingDirectory the directory the Git repository is looked up from
 */
record ReferenceCommitDetector(String referenceCommit, Path workingDirectory) implements FileModificationDetector {

    private static final System.Logger LOG = System.getLogger(ReferenceCommitDetector.class.getName());

    /**
     * @param referenceCommit the configured revision, {@code HEAD} if absent or blank
     * @param workingDirectory the directory the Git repository is looked up from
     */
    static ReferenceCommitDetector of(Optional<String> referenceCommit, Path workingDirectory) {
        var commit = referenceCommit.filter(Predicate.not(String::isBlank));
        commit.ifPresentOrElse(it -> LOG.log(Level.INFO, "Comparing to git commit {0}", it),
                () -> LOG.log(Level.WARNING, "No reference-commit configured, comparing to HEAD."));
        return new ReferenceCommitDetector(commit.orElse("HEAD"), workingDirectory);
    }

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return JGitUtil.withRepository(workingDirectory,
                repository -> JGitUtil.toModifiedFiles(repository, referenceCommit, repository.getFullBranch()));
    }
}
