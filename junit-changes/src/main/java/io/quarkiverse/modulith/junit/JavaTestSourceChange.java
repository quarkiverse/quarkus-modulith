package io.quarkiverse.modulith.junit;

/**
 * A change in a Java test source file.
 *
 * @param fullyQualifiedClassName the name of the changed class
 */
record JavaTestSourceChange(String fullyQualifiedClassName) implements SourceChange {
}
