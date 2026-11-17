package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JGitUtilTests {

    @TempDir
    Path temp;

    @Test
    void findsNoWorkTreeOfABareRepository() throws Exception {
        // arrange
        Git.init().setBare(true).setDirectory(temp.toFile()).call().close();

        // act
        var workTree = JGitUtil.findWorkTree(temp);

        // assert
        assertThat(workTree).isEqualTo(Optional.empty());
    }

    @Test
    void usesTheSecondRevisionWithoutCommonAncestor() throws Exception {
        // arrange
        try (var git = GitRepositories.init(temp)) {
            GitRepositories.write(temp, "main.txt", "1");
            GitRepositories.commitAll(git, "first");
            git.checkout().setOrphan(true).setName("other").call();
            GitRepositories.write(temp, "other.txt", "2");
            GitRepositories.commitAll(git, "second");
        }

        // act
        var base = JGitUtil.withRepository(temp, repository -> JGitUtil.mergeBase(repository, "other", "main"));

        // assert
        assertThat(base).isEqualTo("main");
    }

    @Test
    void rejectsAnUnknownRevision() throws Exception {
        // arrange
        try (var git = GitRepositories.init(temp)) {
            GitRepositories.write(temp, "main.txt", "1");
            GitRepositories.commitAll(git, "first");
        }

        // act
        var thrown = catchThrowable(
                () -> JGitUtil.withRepository(temp, repository -> JGitUtil.mergeBase(repository, "main", "gone")));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to resolve Git revision 'gone'");
    }
}
