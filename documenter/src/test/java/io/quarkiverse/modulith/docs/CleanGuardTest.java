package io.quarkiverse.modulith.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The working directory of every case is {@code work} inside a temporary folder, so that a broken guard can only
 * delete temporary files.
 */
class CleanGuardTest {

    private static final String ANCESTOR = "it is the working directory or one of its ancestors";
    private static final String OUTSIDE_BUILD = "it lies in the working directory outside a sub-folder of target or build";

    @ParameterizedTest
    @CsvSource({
            "work, " + ANCESTOR,
            "'', " + ANCESTOR,
            "work/src, " + OUTSIDE_BUILD,
            "work/src/docs, " + OUTSIDE_BUILD,
            "work/target, " + OUTSIDE_BUILD,
            "work/build, " + OUTSIDE_BUILD,
            "work/targets/docs, " + OUTSIDE_BUILD
    })
    void refusesFoldersHoldingSources(String folder, String reason, @TempDir Path temp) throws IOException {
        // arrange
        Path work = Files.createDirectories(temp.resolve("work"));
        Path output = Files.createDirectories(temp.resolve(folder));

        // act
        Throwable thrown = catchThrowable(() -> CleanGuard.requireCleanable(output, work));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage(refusal(output, reason));
    }

    @Test
    void refusesAFileSystemRoot(@TempDir Path temp) {
        // arrange
        Path root = temp.toAbsolutePath().getRoot();

        // act
        Throwable thrown = catchThrowable(() -> CleanGuard.requireCleanable(root, temp));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(refusal(root, "it is a file system root"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "pom.xml", "build.gradle", "build.gradle.kts" })
    void refusesAProjectFolder(String buildFile, @TempDir Path temp) throws IOException {
        // arrange
        Path work = Files.createDirectories(temp.resolve("work"));
        Path project = Files.createDirectories(temp.resolve("other"));
        Files.writeString(project.resolve(buildFile), "");

        // act
        Throwable thrown = catchThrowable(() -> CleanGuard.requireCleanable(project, work));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(refusal(project, "it holds a " + buildFile));
    }

    @Test
    void refusesALinkFromTheBuildOutputToTheSources(@TempDir Path temp) throws IOException {
        // arrange
        Path work = Files.createDirectories(temp.resolve("work"));
        Path sources = Files.createDirectories(work.resolve("src"));
        Path link = Files.createSymbolicLink(Files.createDirectories(work.resolve("target")).resolve("docs"), sources);

        // act
        Throwable thrown = catchThrowable(() -> CleanGuard.requireCleanable(link, work));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage(refusal(link, OUTSIDE_BUILD));
    }

    @ParameterizedTest
    @ValueSource(strings = { "work/target/modulith-docs", "work/build/modulith-docs", "work/target/a/b", "elsewhere/docs" })
    void acceptsGeneratedFolders(String folder, @TempDir Path temp) throws IOException {
        // arrange
        Path work = Files.createDirectories(temp.resolve("work"));
        Path output = Files.createDirectories(temp.resolve(folder));

        // act
        Throwable thrown = catchThrowable(() -> CleanGuard.requireCleanable(output, work));

        // assert
        assertThat(thrown).isNull();
    }

    @Test
    void folderOutputKeepsARefusedFolder(@TempDir Path temp) throws IOException {
        // arrange
        Path work = Files.createDirectories(temp.resolve("work"));
        Path sources = Files.createDirectories(work.resolve("src"));
        Path source = Files.writeString(sources.resolve("Main.java"), "class Main {}");
        FolderOutput output = new FolderOutput(sources, work);

        // act
        Throwable thrown = catchThrowable(output::clear);

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage(refusal(sources, OUTSIDE_BUILD));
        assertThat(source).hasContent("class Main {}");
    }

    @Test
    void folderOutputEmptiesAnAcceptedFolder(@TempDir Path temp) throws IOException {
        // arrange
        Path work = Files.createDirectories(temp.resolve("work"));
        Path docs = Files.createDirectories(work.resolve("target/modulith-docs/nested"));
        Files.writeString(docs.resolve("stale.puml"), "stale");
        FolderOutput output = new FolderOutput(work.resolve("target/modulith-docs"), work);

        // act
        output.clear();

        // assert
        assertThat(work.resolve("target/modulith-docs")).doesNotExist();
        assertThat(work.resolve("target")).isDirectory();
    }

    private static String refusal(Path folder, String reason) {
        return "Refusing to clean the documentation output " + folder + ": " + reason
                + ". Choose another output folder or use DocumenterOptions.withoutClean().";
    }
}
