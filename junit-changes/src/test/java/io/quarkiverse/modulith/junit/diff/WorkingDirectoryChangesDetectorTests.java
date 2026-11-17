package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkingDirectoryChangesDetectorTests {

    @TempDir
    Path temp;

    @Test
    void filtersFilesContainedInReferenceFolder() {
        // arrange
        var detector = new WorkingDirectoryChangesDetector(() -> ModifiedFile.of("rootPom.xml", "nested/nestedPom.xml"),
                "nested");

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path).containsExactly("nestedPom.xml");
    }

    @Test
    void ignoresSiblingFolderSharingThePrefix() {
        // arrange
        var detector = new WorkingDirectoryChangesDetector(
                () -> ModifiedFile.of("nested-other/pom.xml", "nested/src/A.java", "nested"), "nested");

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path).containsExactly("src/A.java");
    }

    @Test
    void keepsAllFilesForRepositoryRoot() {
        // arrange
        var detector = new WorkingDirectoryChangesDetector(() -> ModifiedFile.of("pom.xml", "nested/pom.xml"), "");

        // act
        var result = detector.getModifiedFiles();

        // assert
        assertThat(result).extracting(ModifiedFile::path).containsExactly("pom.xml", "nested/pom.xml");
    }

    @Test
    void resolvesRepositoryRelativeWorkingDirectoryForNestedMavenModule() throws Exception {
        // arrange
        initGitRepository(temp, "root-pom.xml", "nested-module/module-pom.xml");
        FileModificationDetector delegate = () -> ModifiedFile.of("root-pom.xml", "nested-module/module-pom.xml");

        // act
        var detector = WorkingDirectoryChangesDetector.of(delegate, temp.resolve("nested-module"));

        // assert
        assertThat(detector).contains(new WorkingDirectoryChangesDetector(delegate, "nested-module"));
        assertThat(detector.orElseThrow().getModifiedFiles()).extracting(ModifiedFile::path)
                .containsExactly("module-pom.xml");
    }

    @Test
    void resolvesDeeplyNestedWorkingDirectory() throws Exception {
        // arrange
        initGitRepository(temp, "a/b/pom.xml");
        FileModificationDetector delegate = () -> ModifiedFile.of("a/b/pom.xml");

        // act
        var detector = WorkingDirectoryChangesDetector.of(delegate, temp.resolve("a/./b"));

        // assert
        assertThat(detector).contains(new WorkingDirectoryChangesDetector(delegate, "a/b"));
    }

    @Test
    void resolvesRepositoryRootAsEmptyWorkingDirectory() throws Exception {
        // arrange
        initGitRepository(temp, "pom.xml");
        FileModificationDetector delegate = () -> ModifiedFile.of("pom.xml");

        // act
        var detector = WorkingDirectoryChangesDetector.of(delegate, temp);

        // assert
        assertThat(detector).contains(new WorkingDirectoryChangesDetector(delegate, ""));
    }

    @Test
    void rejectsMissingWorkingDirectoryInsideRepository() throws Exception {
        // arrange
        initGitRepository(temp, "pom.xml");
        FileModificationDetector delegate = () -> ModifiedFile.of("pom.xml");

        // act
        var thrown = catchThrowable(() -> WorkingDirectoryChangesDetector.of(delegate, temp.resolve("missing")));

        // assert
        assertThat(thrown).isExactlyInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(NoSuchFileException.class);
    }

    @Test
    void resolvesNothingOutsideOfGitRepository() {
        // arrange
        FileModificationDetector delegate = () -> ModifiedFile.of("pom.xml");

        // act
        var detector = WorkingDirectoryChangesDetector.of(delegate, temp);

        // assert
        assertThat(detector).isEqualTo(Optional.empty());
    }

    @Test
    void resolvesRepositoryRelativeWorkingDirectoryForGitSubmoduleWorkTree() throws Exception {
        // arrange
        var workTree = initSubmoduleStyleRepository(temp);
        FileModificationDetector delegate = () -> ModifiedFile.of("README", "module-a/module-a-pom.xml",
                "module-b/module-b-pom.xml");

        // act
        var detector = WorkingDirectoryChangesDetector.of(delegate, workTree.resolve("module-a"));

        // assert
        assertThat(detector).contains(new WorkingDirectoryChangesDetector(delegate, "module-a"));
        assertThat(detector.orElseThrow().getModifiedFiles()).extracting(ModifiedFile::path)
                .containsExactly("module-a-pom.xml");
    }

    private static void initGitRepository(Path root, String... files) throws Exception {
        for (var file : files) {
            GitRepositories.write(root, file, file);
        }
        try (var git = GitRepositories.init(root)) {
            GitRepositories.commitAll(git, "init");
        }
    }

    /**
     * Sets up a work tree resembling a Git submodule, whose {@code .git} entry is a file pointing to the Git metadata
     * located in the parent repository's {@code .git/modules} rather than a {@code .git} directory.
     */
    private static Path initSubmoduleStyleRepository(Path temp) throws Exception {
        var gitDir = temp.resolve("parent-repository/.git/modules/example-library");
        var workTree = temp.resolve("example-library");
        Files.createDirectories(gitDir);
        GitRepositories.write(workTree, "README", "example");
        GitRepositories.write(workTree, "module-a/module-a-pom.xml", "module-a");
        GitRepositories.write(workTree, "module-b/module-b-pom.xml", "module-b");
        Git.init().setGitDir(gitDir.toFile()).setDirectory(workTree.toFile()).setInitialBranch("main").call().close();
        Files.writeString(workTree.resolve(".git"), "gitdir: " + gitDir.toAbsolutePath() + "\n");
        try (var repository = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).setWorkTree(workTree.toFile())
                .build(); var git = new Git(repository)) {
            repository.getConfig().setString("core", null, "worktree", workTree.toAbsolutePath().toString());
            repository.getConfig().save();
            GitRepositories.commitAll(git, "init");
        }
        return workTree;
    }
}
