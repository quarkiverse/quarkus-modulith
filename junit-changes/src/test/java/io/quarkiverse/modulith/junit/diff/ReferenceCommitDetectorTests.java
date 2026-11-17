package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.errors.MissingObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReferenceCommitDetectorTests {

    @TempDir
    Path repository;

    String firstCommit;

    @BeforeEach
    void createRepository() throws Exception {
        try (var git = GitRepositories.init(repository)) {
            GitRepositories.write(repository, "modified.txt", "1");
            GitRepositories.write(repository, "deleted.txt", "1");
            GitRepositories.write(repository, "unchanged.txt", "1");
            firstCommit = GitRepositories.commitAll(git, "first").getName();
            GitRepositories.write(repository, "modified.txt", "2");
            Files.delete(repository.resolve("deleted.txt"));
            GitRepositories.write(repository, "src/added.txt", "2");
            GitRepositories.commitAll(git, "second");
            GitRepositories.write(repository, "uncommitted.txt", "3");
        }
    }

    @Test
    void detectsChangesOfHead() {
        // arrange
        var detector = ReferenceCommitDetector.of(Optional.empty(), repository);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(detector.referenceCommit()).isEqualTo("HEAD");
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void comparesToHeadForBlankReferenceCommit(String referenceCommit) {
        // arrange
        var configured = Optional.of(referenceCommit);

        // act
        var detector = ReferenceCommitDetector.of(configured, repository);

        // assert
        assertThat(detector).isEqualTo(new ReferenceCommitDetector("HEAD", repository));
    }

    @Test
    void detectsCommittedChangesSinceReferenceCommit() {
        // arrange
        var detector = ReferenceCommitDetector.of(Optional.of(firstCommit), repository);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(detector.referenceCommit()).isEqualTo(firstCommit);
        assertThat(result).extracting(ModifiedFile::path)
                .containsExactlyInAnyOrder("modified.txt", "deleted.txt", "src/added.txt");
    }

    @Test
    void acceptsRelativeRevisions() {
        // arrange
        var detector = ReferenceCommitDetector.of(Optional.of("HEAD~1"), repository);

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path)
                .containsExactlyInAnyOrder("modified.txt", "deleted.txt", "src/added.txt");
    }

    @Test
    void rejectsUnknownRevision() {
        // arrange
        var detector = ReferenceCommitDetector.of(Optional.of("does-not-exist"), repository);

        // act
        var thrown = catchThrowable(detector::getModifiedFiles);

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to resolve Git revision 'does-not-exist'");
    }

    @Test
    void rejectsMissingCommit() {
        // arrange
        var detector = ReferenceCommitDetector.of(Optional.of("0123456789012345678901234567890123456789"), repository);

        // act
        var thrown = catchThrowable(detector::getModifiedFiles);

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to inspect the Git repository at or above " + repository)
                .hasCauseExactlyInstanceOf(MissingObjectException.class);
    }
}
