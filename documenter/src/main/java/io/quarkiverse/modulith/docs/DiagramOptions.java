package io.quarkiverse.modulith.docs;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;

/**
 * Options for the PlantUML diagrams, Spring Modulith's {@code Documenter.DiagramOptions}. Instances are immutable,
 * every {@code with...} method returns a new instance.
 */
public final class DiagramOptions {

    private final EnumSet<DependencyType> dependencyTypes;
    private final DependencyDepth dependencyDepth;
    private final Predicate<ApplicationModule> exclusions;
    private final Predicate<DiagramComponent> componentFilter;
    private final Predicate<ApplicationModule> targetOnly;
    private final Optional<String> targetFileName;
    private final Function<ApplicationModule, Optional<String>> colorSelector;
    private final Function<ApplicationModule, String> defaultDisplayName;
    private final DiagramStyle style;
    private final ElementsWithoutRelationships elementsWithoutRelationships;
    private final Map<String, String> skinParams;

    private DiagramOptions(EnumSet<DependencyType> dependencyTypes, DependencyDepth dependencyDepth,
            Predicate<ApplicationModule> exclusions, Predicate<DiagramComponent> componentFilter,
            Predicate<ApplicationModule> targetOnly, Optional<String> targetFileName,
            Function<ApplicationModule, Optional<String>> colorSelector,
            Function<ApplicationModule, String> defaultDisplayName, DiagramStyle style,
            ElementsWithoutRelationships elementsWithoutRelationships, Map<String, String> skinParams) {
        this.dependencyTypes = Objects.requireNonNull(dependencyTypes, "dependencyTypes");
        this.dependencyDepth = Objects.requireNonNull(dependencyDepth, "dependencyDepth");
        this.exclusions = Objects.requireNonNull(exclusions, "exclusions");
        this.componentFilter = Objects.requireNonNull(componentFilter, "componentFilter");
        this.targetOnly = Objects.requireNonNull(targetOnly, "targetOnly");
        this.targetFileName = targetFileName;
        this.colorSelector = Objects.requireNonNull(colorSelector, "colorSelector");
        this.defaultDisplayName = Objects.requireNonNull(defaultDisplayName, "defaultDisplayName");
        this.style = Objects.requireNonNull(style, "style");
        this.elementsWithoutRelationships = Objects.requireNonNull(elementsWithoutRelationships,
                "elementsWithoutRelationships");
        this.skinParams = skinParams;
    }

    /**
     * @return all dependency types, immediate dependencies, nothing excluded, C4 style, modules without relationships
     *         hidden, the display name as label
     */
    public static DiagramOptions defaults() {
        return new DiagramOptions(EnumSet.allOf(DependencyType.class), DependencyDepth.IMMEDIATE, it -> false,
                it -> true, it -> false, Optional.empty(), it -> Optional.empty(), ApplicationModule::getDisplayName,
                DiagramStyle.C4, ElementsWithoutRelationships.HIDDEN, Map.of());
    }

    /**
     * @return options showing the module dependencies up to the given depth in per-module diagrams
     */
    public DiagramOptions withDependencyDepth(DependencyDepth dependencyDepth) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    /**
     * @return options leaving out the modules matching the predicate
     */
    public DiagramOptions withExclusions(Predicate<ApplicationModule> exclusions) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    /**
     * @return options keeping only the components matching the predicate
     */
    public DiagramOptions withComponentFilter(Predicate<DiagramComponent> componentFilter) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    /**
     * @return options hiding the outgoing relationships of the modules matching the predicate
     */
    public DiagramOptions withTargetOnly(Predicate<ApplicationModule> targetOnly) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    /**
     * As in Spring Modulith, the diagram of all modules is written to the pattern itself, unformatted: with
     * {@code diagram-%s.puml} it goes to a file named {@code diagram-%s.puml}, which {@code all-docs.adoc} includes.
     *
     * @param targetFileName a file name pattern with a {@code %s} placeholder for the module identifier
     * @return options writing the diagrams to the given file names
     */
    public DiagramOptions withTargetFileName(String targetFileName) {
        if (!targetFileName.contains("%s")) {
            throw new IllegalArgumentException("Configured file name pattern '" + targetFileName
                    + "' does not include a '%s' placeholder for the module name!");
        }
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                Optional.of(targetFileName), colorSelector, defaultDisplayName, style, elementsWithoutRelationships,
                skinParams);
    }

    /**
     * @param colorSelector the background color per module, as a hex code ({@code #ff0000}) or an HTML color name
     * @return options coloring the modules
     */
    public DiagramOptions withColorSelector(Function<ApplicationModule, Optional<String>> colorSelector) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    /**
     * @return options labelling the modules with the given function
     */
    public DiagramOptions withDefaultDisplayName(Function<ApplicationModule, String> defaultDisplayName) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    public DiagramOptions withStyle(DiagramStyle style) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    public DiagramOptions withElementsWithoutRelationships(ElementsWithoutRelationships elementsWithoutRelationships) {
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    /**
     * @param name the PlantUML skin parameter, must not be blank
     * @param value its value
     * @return options adding the skin parameter to every diagram
     */
    public DiagramOptions withSkinParam(String name, String value) {
        if (Objects.isNull(name) || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or empty!");
        }
        Map<String, String> params = new LinkedHashMap<>(skinParams);
        params.put(name, value);
        return new DiagramOptions(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly,
                targetFileName, colorSelector, defaultDisplayName, style, elementsWithoutRelationships,
                Collections.unmodifiableMap(params));
    }

    /**
     * @return options showing only relationships of the given types
     */
    public DiagramOptions withDependencyTypes(DependencyType... types) {
        EnumSet<DependencyType> selected = EnumSet.noneOf(DependencyType.class);
        selected.addAll(Arrays.asList(types));
        return new DiagramOptions(selected, dependencyDepth, exclusions, componentFilter, targetOnly, targetFileName,
                colorSelector, defaultDisplayName, style, elementsWithoutRelationships, skinParams);
    }

    EnumSet<DependencyType> dependencyTypes() {
        return dependencyTypes;
    }

    DependencyDepth dependencyDepth() {
        return dependencyDepth;
    }

    Predicate<ApplicationModule> exclusions() {
        return exclusions;
    }

    Predicate<DiagramComponent> componentFilter() {
        return componentFilter;
    }

    Predicate<ApplicationModule> targetOnly() {
        return targetOnly;
    }

    Optional<String> targetFileName() {
        return targetFileName;
    }

    Function<ApplicationModule, Optional<String>> colorSelector() {
        return colorSelector;
    }

    Function<ApplicationModule, String> defaultDisplayName() {
        return defaultDisplayName;
    }

    DiagramStyle style() {
        return style;
    }

    boolean hideElementsWithoutRelationships() {
        return elementsWithoutRelationships == ElementsWithoutRelationships.HIDDEN;
    }

    Map<String, String> skinParams() {
        return skinParams;
    }
}
