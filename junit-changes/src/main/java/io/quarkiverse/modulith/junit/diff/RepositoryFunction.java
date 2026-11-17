package io.quarkiverse.modulith.junit.diff;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * A function over an open Git {@link Repository} that may fail with JGit's checked exceptions.
 *
 * @param <T> the result type
 */
@FunctionalInterface
interface RepositoryFunction<T> {

    T apply(Repository repository) throws IOException, GitAPIException;
}
