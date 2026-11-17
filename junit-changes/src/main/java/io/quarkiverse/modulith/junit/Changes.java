package io.quarkiverse.modulith.junit;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.junit.diff.ModifiedFile;

/**
 * The set of {@link Change}s made to a project, as detected by a
 * {@link io.quarkiverse.modulith.junit.diff.FileModificationDetector}.
 */
public final class Changes implements Iterable<Change> {

    /**
     * No changes at all, executing all tests.
     */
    public static final Changes NONE = new Changes(Set.of(), OnNoChange.EXECUTE_ALL);

    private final Collection<Change> changes;
    private final OnNoChange onNoChange;

    private Changes(Collection<Change> changes, OnNoChange onNoChange) {
        this.changes = changes;
        this.onNoChange = onNoChange;
    }

    /**
     * Creates the changes from the given modified files.
     */
    static Changes of(Stream<ModifiedFile> files, OnNoChange onNoChange) {
        return new Changes(files.map(Changes::toChange).collect(Collectors.toUnmodifiableSet()), onNoChange);
    }

    /**
     * Classifies a modified file as Java source, Kotlin source or other file change.
     */
    static Change toChange(ModifiedFile file) {
        if (file.isJavaSource()) {
            return LanguageConfig.JAVA.toChange(file);
        }
        return file.isKotlinSource() ? LanguageConfig.KOTLIN.toChange(file) : new OtherFileChange(file.path());
    }

    /**
     * @return whether there are no changes at all
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * @return the fully qualified names of the changed main and test classes
     */
    public Set<String> getChangedClasses() {
        return changes.stream()
                .filter(SourceChange.class::isInstance)
                .map(SourceChange.class::cast)
                .map(SourceChange::fullyQualifiedClassName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return whether a main or test class changed
     */
    public boolean hasClassChanges() {
        return !getChangedClasses().isEmpty();
    }

    /**
     * @return whether tests are skipped when no class, class path resource or build file changed, as configured by
     *         {@code quarkus.modulith.test.on-no-changes=skip-all}
     */
    public boolean skipTestsOnNoChanges() {
        return onNoChange == OnNoChange.SKIP_ALL;
    }

    /**
     * @param type the type to look up
     * @return whether the source of the given type changed
     */
    public boolean contains(Class<?> type) {
        return changes.stream().anyMatch(it -> it.hasOrigin(type.getName()));
    }

    /**
     * @return whether a build file of the module changed, see the back-off rules of change-aware test execution
     */
    public boolean hasBuildResourceChanges() {
        return otherFileChanges().anyMatch(OtherFileChange::affectsBuildResource);
    }

    /**
     * @return whether a file below {@code src/main/resources} or {@code src/test/resources} changed
     */
    public boolean hasClasspathResourceChange() {
        return otherFileChanges().anyMatch(OtherFileChange::isClasspathResource);
    }

    @Override
    public Iterator<Change> iterator() {
        return changes.iterator();
    }

    @Override
    public String toString() {
        return changes.stream().map(Object::toString).sorted().collect(Collectors.joining("\n"));
    }

    private Stream<OtherFileChange> otherFileChanges() {
        return changes.stream().filter(OtherFileChange.class::isInstance).map(OtherFileChange.class::cast);
    }
}
