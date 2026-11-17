package io.quarkiverse.modulith.junit.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * JGit operations shared by the detectors. For internal use only.
 */
final class JGitUtil {

    private JGitUtil() {
    }

    /**
     * @param directory the directory to start the lookup at
     * @return the root of the work tree of the Git repository at or above the given directory, empty if there is no
     *         repository or only a bare one
     */
    static Optional<Path> findWorkTree(Path directory) {
        var builder = new FileRepositoryBuilder().findGitDir(directory.toFile());
        return Optional.ofNullable(builder.getGitDir())
                .flatMap(gitDir -> apply(builder, directory, repository -> repository.isBare() ? Optional.empty()
                        : Optional.of(repository.getWorkTree().toPath())));
    }

    /**
     * Opens the Git repository at or above the given directory, applies the function and closes the repository.
     *
     * @throws IllegalStateException wrapping JGit's checked exceptions
     */
    static <T> T withRepository(Path directory, RepositoryFunction<T> function) {
        return apply(new FileRepositoryBuilder().findGitDir(directory.toFile()), directory, function);
    }

    /**
     * @return the revision of the best common ancestor of the two revisions, the second revision if they have none
     * @throws IllegalStateException if a revision cannot be resolved
     */
    static String mergeBase(Repository repository, String first, String second) throws IOException {
        try (var walk = new RevWalk(repository)) {
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(walk.parseCommit(resolve(repository, first)));
            walk.markStart(walk.parseCommit(resolve(repository, second)));
            return Optional.ofNullable(walk.next()).map(RevCommit::name).orElse(second);
        }
    }

    private static <T> T apply(FileRepositoryBuilder builder, Path directory, RepositoryFunction<T> function) {
        try (var repository = builder.setMustExist(true).build()) {
            return function.apply(repository);
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException("Unable to inspect the Git repository at or above %s".formatted(directory), e);
        }
    }

    /**
     * @return the files that differ between the trees of the two revisions, each path once
     */
    static Stream<ModifiedFile> toModifiedFiles(Repository repository, String oldRef, String newRef)
            throws IOException, GitAPIException {
        try (var git = new Git(repository)) {
            var entries = git.diff()
                    .setOldTree(prepareTreeParser(repository, oldRef))
                    .setNewTree(prepareTreeParser(repository, newRef))
                    .call();
            return entries.stream()
                    .flatMap(entry -> ModifiedFile.of(entry.getNewPath(), entry.getOldPath()))
                    .distinct()
                    .filter(file -> !DiffEntry.DEV_NULL.equals(file.path()))
                    .toList()
                    .stream();
        }
    }

    private static ObjectId resolve(Repository repository, String ref) throws IOException {
        var commitId = repository.resolve(ref);
        if (Objects.isNull(commitId)) {
            throw new IllegalStateException("Unable to resolve Git revision '%s'".formatted(ref));
        }
        return commitId;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
        var commitId = resolve(repository, ref);
        try (var walk = new RevWalk(repository); var reader = repository.newObjectReader()) {
            var treeParser = new CanonicalTreeParser();
            treeParser.reset(reader, walk.parseCommit(commitId).getTree().getId());
            return treeParser;
        }
    }
}
