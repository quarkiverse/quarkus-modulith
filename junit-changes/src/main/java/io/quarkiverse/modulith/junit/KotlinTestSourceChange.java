package io.quarkiverse.modulith.junit;

/**
 * A change in a Kotlin test source file.
 *
 * @param fullyQualifiedClassName the name of the changed class
 */
record KotlinTestSourceChange(String fullyQualifiedClassName) implements SourceChange {
}
