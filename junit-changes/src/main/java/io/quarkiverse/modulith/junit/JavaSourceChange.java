package io.quarkiverse.modulith.junit;

/**
 * A change in a Java source file.
 *
 * @param fullyQualifiedClassName the name of the changed class
 */
record JavaSourceChange(String fullyQualifiedClassName) implements SourceChange {
}
