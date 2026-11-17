package io.quarkiverse.modulith.junit;

/**
 * A change to the local project: a Java or Kotlin source file, or any other file.
 */
public sealed interface Change permits SourceChange, OtherFileChange {

    /**
     * @param nameOrPath a fully qualified class name or a file path
     * @return whether the change originates from the given class or file
     */
    boolean hasOrigin(String nameOrPath);
}
