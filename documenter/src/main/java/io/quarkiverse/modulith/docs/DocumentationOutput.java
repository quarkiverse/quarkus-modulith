package io.quarkiverse.modulith.docs;

/**
 * Where the {@link Documenter} writes its files. The default writes to a folder, the Quarkus build collects the files
 * in memory and hands them to the build output.
 */
public interface DocumentationOutput {

    /**
     * @return whether a file with the given name was written
     */
    boolean contains(String name);

    /**
     * Writes or replaces the file with the given name.
     */
    void write(String name, String content);

    /**
     * Removes every file.
     */
    void clear();
}
