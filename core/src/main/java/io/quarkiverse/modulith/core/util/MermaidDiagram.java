package io.quarkiverse.modulith.core.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;

/**
 * Renders the module dependencies as a Mermaid flowchart, a Quarkus extra: a node per module, a dotted
 * {@code listens to} arrow when every reference to the target module is an event type the module listens to, a
 * {@code uses} arrow otherwise, and dashed borders for open modules. The build records the same diagram for
 * {@link ApplicationModules#toMermaid()}.
 */
public final class MermaidDiagram {

    private MermaidDiagram() {
    }

    public static String of(ApplicationModules modules) {
        List<ApplicationModule> sorted = modules.stream().sorted(Comparator.comparing(ApplicationModule::getName))
                .toList();
        Map<String, String> ids = uniqueIds(sorted);
        StringBuilder out = new StringBuilder("flowchart LR\n");
        for (ApplicationModule module : sorted) {
            out.append("    ").append(ids.get(module.getName())).append("[\"")
                    .append(module.getDisplayName().replace("\"", "#quot;")).append("\"]\n");
        }
        for (ApplicationModule module : sorted) {
            Map<String, Set<String>> targets = new TreeMap<>();
            module.getDirectDependencies(modules).stream().forEach(it -> targets
                    .computeIfAbsent(it.getTargetModule().getName(), key -> new TreeSet<>()).add(it.getTargetType()));
            targets.forEach((target, types) -> {
                String arrow = module.getListenedEvents().containsAll(types) ? " -. \"listens to\" .-> "
                        : " -- \"uses\" --> ";
                out.append("    ").append(ids.get(module.getName())).append(arrow).append(ids.get(target)).append('\n');
            });
        }
        List<String> open = sorted.stream().filter(ApplicationModule::isOpen).map(it -> ids.get(it.getName())).toList();
        if (!open.isEmpty()) {
            out.append("    classDef open stroke-dasharray: 5 5\n");
            out.append("    class ").append(String.join(",", open)).append(" open\n");
        }
        return out.toString();
    }

    /**
     * @return the node id of each module, those that {@link #id(String)} maps to the same id suffixed with
     *         {@code _2}, {@code _3} and so on in name order
     */
    private static Map<String, String> uniqueIds(List<ApplicationModule> sorted) {
        Map<String, String> ids = new HashMap<>();
        Set<String> used = new HashSet<>();
        sorted.forEach(module -> {
            String id = id(module.getName());
            String unique = used.contains(id)
                    ? Stream.iterate(2, it -> it + 1).map(it -> id + "_" + it).filter(it -> !used.contains(it))
                            .findFirst().orElseThrow()
                    : id;
            used.add(unique);
            ids.put(module.getName(), unique);
        });
        return ids;
    }

    /**
     * @return the module name as a Mermaid node id: other characters than letters, digits and underscores replaced,
     *         and a leading digit prefixed
     */
    public static String id(String name) {
        String id = name.replaceAll("[^A-Za-z0-9_]", "_");
        return Character.isDigit(id.charAt(0)) ? "m_" + id : id;
    }
}
