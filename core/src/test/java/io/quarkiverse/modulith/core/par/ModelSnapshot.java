package io.quarkiverse.modulith.core.par;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The parts of a module model the parity tests compare, as sorted text lines.
 *
 * @param modules identifier, display name, base package, open flag and parent of each module
 * @param namedInterfaces the types of each named interface
 * @param dependencies the direct dependencies with their types
 * @param violations the first line of each violation message, cycles as the set of modules involved
 * @param details events, aggregate roots, value types and beans
 */
record ModelSnapshot(Set<String> modules, Set<String> namedInterfaces, Set<String> dependencies, Set<String> violations,
        Set<String> details) {

    static Set<String> sorted(List<String> lines) {
        return new TreeSet<>(lines);
    }

    /**
     * @return the first line of a violation message, or the sorted modules of a cycle
     */
    static String violation(String message) {
        if (message.startsWith("Cycle detected")) {
            String path = message.lines().takeWhile(it -> !it.contains("Dependencies of")).reduce("", String::concat);
            return "cycle " + new TreeSet<>(List.of(path.replace("Cycle detected:", "").replace("Slice", "")
                    .replaceAll("\\s", "").split("->")));
        }
        return message.lines().findFirst().orElseThrow();
    }
}
