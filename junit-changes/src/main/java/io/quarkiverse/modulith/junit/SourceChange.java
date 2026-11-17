package io.quarkiverse.modulith.junit;

/**
 * A change to a source file, identified by the fully qualified name of the class it declares.
 */
sealed interface SourceChange extends Change
        permits JavaSourceChange, JavaTestSourceChange, KotlinSourceChange, KotlinTestSourceChange {

    String fullyQualifiedClassName();

    @Override
    default boolean hasOrigin(String nameOrPath) {
        return fullyQualifiedClassName().equals(nameOrPath);
    }
}
