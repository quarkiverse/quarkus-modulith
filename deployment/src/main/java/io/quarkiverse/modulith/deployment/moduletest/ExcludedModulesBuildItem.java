package io.quarkiverse.modulith.deployment.moduletest;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The base packages of the modules left out of the current module test. Empty outside module tests.
 */
public final class ExcludedModulesBuildItem extends SimpleBuildItem {

    private static final int NO_MATCH = -1;

    private final Set<String> packages;
    private final Set<String> keptPackages;

    /**
     * @param packages the base packages of the modules left out
     * @param keptPackages the base packages of started modules nested inside a module that is left out
     */
    public ExcludedModulesBuildItem(Set<String> packages, Set<String> keptPackages) {
        this.packages = Set.copyOf(packages);
        this.keptPackages = Set.copyOf(keptPackages);
    }

    public Set<String> getPackages() {
        return packages;
    }

    public Set<String> getKeptPackages() {
        return keptPackages;
    }

    /**
     * @return whether the class belongs to an excluded module: the closest module package containing it is the one
     *         of an excluded module, so the types of a started module nested inside an excluded one are kept
     */
    public boolean excludes(String className) {
        return closest(packages, className) > closest(keptPackages, className);
    }

    private static int closest(Set<String> candidates, String className) {
        return candidates.stream().filter(it -> className.startsWith(it + ".")).mapToInt(String::length).max()
                .orElse(NO_MATCH);
    }
}
