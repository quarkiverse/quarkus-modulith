package io.quarkiverse.modulith.junit;

/**
 * A change in a Kotlin source file.
 *
 * @param fullyQualifiedClassName the name of the changed class
 */
record KotlinSourceChange(String fullyQualifiedClassName) implements SourceChange {
}
