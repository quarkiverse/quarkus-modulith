package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnpushedCommitsDetectorTests {

    @TempDir
    Path temp;

    @Test
    void detectsFilesOfCommitsNotPushedToTrackingBranch() throws Exception {
        // arrange
        var remote = temp.resolve("remote");
        var local = temp.resolve("local");
        try (var git = GitRepositories.init(remote)) {
            GitRepositories.write(remote, "pushed.txt", "1");
            GitRepositories.commitAll(git, "first");
        }
        try (var git = GitRepositories.cloneOf(remote, local)) {
            GitRepositories.write(local, "pushed.txt", "2");
            GitRepositories.write(local, "added.txt", "2");
            GitRepositories.commitAll(git, "second");
        }
        GitRepositories.write(local, "uncommitted.txt", "3");
        var detector = new UnpushedCommitsDetector(local);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path).containsExactlyInAnyOrder("pushed.txt", "added.txt");
    }

    @Test
    void ignoresCommitsOnlyTheTrackingBranchHas() throws Exception {
        // arrange
        var remote = temp.resolve("remote");
        var local = temp.resolve("local");
        try (var git = GitRepositories.init(remote)) {
            GitRepositories.write(remote, "pushed.txt", "1");
            GitRepositories.commitAll(git, "first");
        }
        try (var git = GitRepositories.cloneOf(remote, local)) {
            try (var upstream = Git.open(remote.toFile())) {
                GitRepositories.write(remote, "fetched.txt", "2");
                GitRepositories.commitAll(upstream, "second");
            }
            git.fetch().call();
            GitRepositories.write(local, "unpushed.txt", "3");
            GitRepositories.commitAll(git, "third");
        }
        var detector = new UnpushedCommitsDetector(local);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path).containsExactly("unpushed.txt");
    }

    @Test
    void detectsNothingWithoutTrackingBranch() throws Exception {
        // arrange
        try (var git = GitRepositories.init(temp)) {
            GitRepositories.write(temp, "file.txt", "1");
            GitRepositories.commitAll(git, "first");
            GitRepositories.write(temp, "file.txt", "2");
            GitRepositories.commitAll(git, "second");
        }
        var detector = new UnpushedCommitsDetector(temp);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).isEmpty();
    }
}
