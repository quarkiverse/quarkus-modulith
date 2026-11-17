package io.quarkiverse.modulith.junit.diff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Creates temporary Git repositories for tests, independent of the user's Git configuration.
 */
public final class GitRepositories {

    private static final PersonIdent AUTHOR = new PersonIdent("Test", "test@example.com");

    private GitRepositories() {
    }

    public static Git init(Path directory) throws GitAPIException {
        return Git.init().setDirectory(directory.toFile()).setInitialBranch("main").call();
    }

    public static Git cloneOf(Path remote, Path directory) throws GitAPIException {
        return Git.cloneRepository().setURI(remote.toUri().toString()).setDirectory(directory.toFile()).call();
    }

    public static void write(Path root, String file, String content) throws IOException {
        var path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    public static RevCommit commitAll(Git git, String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.add().addFilepattern(".").setUpdate(true).call();
        return git.commit().setSign(false).setAuthor(AUTHOR).setCommitter(AUTHOR).setMessage(message).call();
    }
}
