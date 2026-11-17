package io.quarkiverse.modulith.deployment.moduletest;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which modules a module test starts.
 *
 * @param module the module under test
 * @param mode the bootstrap mode
 * @param dependencies the modules started because of the bootstrap mode, in discovery order
 * @param extraModules the modules the test asked for in addition
 * @param sharedModules the shared modules, always started
 * @param excludedPackages the base packages of the modules that are left out
 * @param keptPackages the base packages of started modules nested inside a module that is left out
 * @param excludedTypes the type patterns of the beans that are left out, in the format of ArC's
 *        {@code ExcludedTypeBuildItem}
 */
public record ModuleTestSetup(String module, ModuleTestConfig.BootstrapMode mode, List<String> dependencies,
        List<String> extraModules, List<String> sharedModules, Set<String> excludedPackages, Set<String> keptPackages,
        Set<String> excludedTypes) {

    public ModuleTestSetup {
        dependencies = List.copyOf(dependencies);
        extraModules = List.copyOf(extraModules);
        sharedModules = List.copyOf(sharedModules);
        excludedPackages = Collections.unmodifiableSet(new LinkedHashSet<>(excludedPackages));
        keptPackages = Collections.unmodifiableSet(new LinkedHashSet<>(keptPackages));
        excludedTypes = Collections.unmodifiableSet(new LinkedHashSet<>(excludedTypes));
    }

    /**
     * @return the started modules: the module under test, its dependencies, the extra and the shared modules
     */
    public Set<String> includedModules() {
        Set<String> included = new LinkedHashSet<>();
        included.add(module);
        included.addAll(dependencies);
        included.addAll(extraModules);
        included.addAll(sharedModules);
        return included;
    }

    /**
     * @return the content of the class path resource {@code quarkus-modulith-junit} reads at runtime, in the
     *         {@link java.util.Properties} format, ASCII only: separators, comment characters, backslashes,
     *         whitespace and every character outside printable ASCII are escaped
     */
    public String describe() {
        return line("module", module) + line("bootstrap-mode", mode.name())
                + line("dependencies", String.join(",", dependencies))
                + line("extra-includes", String.join(",", extraModules))
                + line("shared-modules", String.join(",", sharedModules));
    }

    private static String line(String key, String value) {
        return key + "=" + escape(value) + "\n";
    }

    static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            if ("\\:=#! ".indexOf(c) >= 0) {
                escaped.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                escaped.append(String.format("\\u%04x", (int) c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
