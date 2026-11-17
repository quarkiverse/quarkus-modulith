package io.quarkiverse.modulith.docs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Options for the module canvases, Spring Modulith's {@code Documenter.CanvasOptions}. Instances are immutable.
 */
public final class CanvasOptions {

    static final Grouping FALLBACK_GROUP = Grouping.of("Others", it -> true);

    private static final String DEFAULT_FILE_NAME = "module-%s.adoc";

    private final List<Grouping> groupings;
    private final Optional<String> apiBase;
    private final Optional<String> targetFileName;
    private final boolean hideInternals;
    private final boolean hideEmptyLines;

    private CanvasOptions(List<Grouping> groupings, Optional<String> apiBase, Optional<String> targetFileName,
            boolean hideInternals, boolean hideEmptyLines) {
        this.groupings = List.copyOf(groupings);
        this.apiBase = apiBase;
        this.targetFileName = targetFileName;
        this.hideInternals = hideInternals;
        this.hideEmptyLines = hideEmptyLines;
    }

    /**
     * @return the jMolecules groupings followed by the {@link QuarkusGroupings}, internals and empty rows hidden
     */
    public static CanvasOptions defaults() {
        return withoutDefaultGroupings()
                .groupingBy(JMoleculesGroupings.getGroupings())
                .groupingBy(QuarkusGroupings.getGroupings());
    }

    /**
     * @return options listing beans without groups, internals and empty rows hidden
     */
    public static CanvasOptions withoutDefaultGroupings() {
        return new CanvasOptions(List.of(), Optional.empty(), Optional.empty(), true, true);
    }

    /**
     * @return options adding the given groups after the existing ones; a bean goes to the first group it matches
     */
    public CanvasOptions groupingBy(Grouping... groupings) {
        List<Grouping> result = new ArrayList<>(this.groupings);
        result.addAll(Arrays.asList(groupings));
        return new CanvasOptions(result, apiBase, targetFileName, hideInternals, hideEmptyLines);
    }

    public CanvasOptions groupingBy(String name, Predicate<ModuleBean> filter) {
        return groupingBy(Grouping.of(name, filter));
    }

    public CanvasOptions groupingBy(String name, Predicate<ModuleBean> filter, String description) {
        if (Objects.isNull(description) || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be null!");
        }
        return groupingBy(Grouping.of(name, filter, description));
    }

    /**
     * @return options listing the internal beans and types of a module as well
     */
    public CanvasOptions revealInternals() {
        return new CanvasOptions(groupings, apiBase, targetFileName, false, hideEmptyLines);
    }

    /**
     * @return options writing rows without content too
     */
    public CanvasOptions revealEmptyLines() {
        return new CanvasOptions(groupings, apiBase, targetFileName, hideInternals, false);
    }

    /**
     * @param apiBase the base URL of the Javadoc, types are linked to it
     */
    public CanvasOptions withApiBase(String apiBase) {
        if (Objects.isNull(apiBase) || apiBase.isBlank()) {
            throw new IllegalArgumentException("API base must not be null or empty!");
        }
        return new CanvasOptions(groupings, Optional.of(apiBase), targetFileName, hideInternals, hideEmptyLines);
    }

    /**
     * @param targetFileName a file name pattern with a {@code %s} placeholder for the module identifier
     */
    public CanvasOptions withTargetFileName(String targetFileName) {
        return new CanvasOptions(groupings, apiBase, Optional.ofNullable(targetFileName), hideInternals,
                hideEmptyLines);
    }

    Optional<String> apiBase() {
        return apiBase;
    }

    boolean hideEmptyLines() {
        return hideEmptyLines;
    }

    String targetFileName(String moduleIdentifier) {
        return targetFileName.orElse(DEFAULT_FILE_NAME).formatted(moduleIdentifier);
    }

    /**
     * @return the beans of the module per group, in group order, groups without beans left out; a bean goes to the
     *         first group it matches, unmatched beans to the fallback group
     */
    Map<Grouping, List<ModuleBean>> groupBeans(ModuleDetails module) {
        List<Grouping> sources = new ArrayList<>(groupings);
        sources.add(FALLBACK_GROUP);
        Map<Grouping, List<ModuleBean>> result = new LinkedHashMap<>();
        List<ModuleBean> mapped = new ArrayList<>();
        for (Grouping grouping : sources) {
            List<ModuleBean> matching = module.beans().stream()
                    .filter(bean -> !hideInternals || bean.apiBean())
                    .filter(bean -> !mapped.contains(bean))
                    .filter(grouping::matches)
                    .toList();
            if (!matching.isEmpty()) {
                result.put(grouping, matching);
                mapped.addAll(matching);
            }
        }
        return result;
    }

    /**
     * @return whether a type is shown: always with revealed internals, otherwise only exposed types
     */
    boolean shows(ModuleType type) {
        return !hideInternals || type.exposed();
    }
}
