package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UncommittedChangesDetectorTests {

    @TempDir
    Path repository;

    @Test
    void detectsStagedModifiedDeletedAndUntrackedFiles() throws Exception {
        // arrange
        try (var git = GitRepositories.init(repository)) {
            GitRepositories.write(repository, "modified.txt", "1");
            GitRepositories.write(repository, "deleted.txt", "1");
            GitRepositories.write(repository, "unchanged.txt", "1");
            GitRepositories.commitAll(git, "first");
            GitRepositories.write(repository, "modified.txt", "2");
            Files.delete(repository.resolve("deleted.txt"));
            GitRepositories.write(repository, "staged.txt", "2");
            git.add().addFilepattern("staged.txt").call();
            GitRepositories.write(repository, "nested/untracked.txt", "2");
        }
        var detector = new UncommittedChangesDetector(repository.resolve("nested"));

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path)
                .containsExactlyInAnyOrder("modified.txt", "deleted.txt", "staged.txt", "nested/untracked.txt");
    }

    @Test
    void detectsNothingInCleanWorkingTree() throws Exception {
        // arrange
        try (var git = GitRepositories.init(repository)) {
            GitRepositories.write(repository, "file.txt", "1");
            GitRepositories.commitAll(git, "first");
        }
        var detector = new UncommittedChangesDetector(repository);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).isEmpty();
    }
}
