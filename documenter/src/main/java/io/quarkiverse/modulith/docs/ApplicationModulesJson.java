package io.quarkiverse.modulith.docs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.DependencyType;

/**
 * Writes the module model as JSON, in the format of Spring Modulith's {@code ApplicationModulesExporter}: the simple
 * form is what the {@code /actuator/modulith} endpoint returns, the full form adds the named interfaces and the
 * initializers. Keys keep Spring Modulith's order; dependency types and named interface types are sorted.
 */
public final class ApplicationModulesJson {

    private static final int FIRST_PRINTABLE = 0x20;

    private ApplicationModulesJson() {
    }

    /**
     * @return per module: display name, base package, parent, nested modules, type, shared, allowed dependencies and
     *         dependencies with their types
     */
    public static String simple(DocumentationModel model) {
        return toJson(model, false);
    }

    /**
     * @return the {@link #simple(DocumentationModel)} form plus the named interfaces and initializers of each module
     */
    public static String full(DocumentationModel model) {
        return toJson(model, true);
    }

    private static String toJson(DocumentationModel model, boolean full) {
        Map<String, Object> result = new LinkedHashMap<>();
        model.modules().forEach(module -> result.put(module.getName(), toInfo(model, module, full)));
        return write(result);
    }

    private static Map<String, Object> toInfo(DocumentationModel model, ApplicationModule module, boolean full) {
        Map<String, Object> json = new LinkedHashMap<>();
        List<ApplicationModule> shared = model.sharedModules();
        ModuleDetails details = model.details(module);
        json.put("displayName", module.getDisplayName());
        json.put("basePackage", module.getBasePackage());
        model.parentOf(module).ifPresent(it -> json.put("parent", it.getName()));
        json.put("nested", model.nestedModules(module).stream().map(ApplicationModule::getName).toList());
        json.put("type", module.isOpen() ? "open" : "closed");
        json.put("shared", shared.contains(module));
        if (full) {
            Map<String, Object> namedInterfaces = new TreeMap<>();
            details.namedInterfaces().forEach((name, types) -> namedInterfaces.put(name, new TreeSet<>(types)));
            json.put("namedInterfaces", namedInterfaces);
            json.put("initializers", details.initializers());
        }
        allowedDependencies(module, shared).ifPresent(it -> json.put("allowedDependencies", it));
        List<Object> dependencies = new ArrayList<>();
        model.dependencyTypesByModule(module).forEach((target, types) -> {
            Map<String, Object> dependency = new LinkedHashMap<>();
            dependency.put("target", target.getName());
            dependency.put("types", types.stream().map(DependencyType::name).toList());
            dependencies.add(dependency);
        });
        json.put("dependencies", dependencies);
        return json;
    }

    /**
     * @return the declared allowed dependencies without the shared modules, empty when the module is unrestricted or
     *         neither declares a dependency nor has a shared module to depend on
     */
    private static Optional<List<String>> allowedDependencies(ApplicationModule module, List<ApplicationModule> shared) {
        List<String> sharedNames = shared.stream().filter(it -> !it.equals(module)).map(ApplicationModule::getName)
                .toList();
        return module.getAllowedDependencies()
                .filter(declared -> !declared.isEmpty() || !sharedNames.isEmpty())
                .map(declared -> declared.stream()
                        .map(ApplicationModulesJson::normalize)
                        .filter(it -> !sharedNames.contains(it.split(" :: ")[0]))
                        .distinct()
                        .toList());
    }

    /**
     * @return {@code target}, {@code target :: interface} or {@code target :: *}, Spring Modulith's notation
     */
    private static String normalize(String declaration) {
        String[] segments = declaration.split("::");
        String target = segments[0].trim();
        return segments.length > 1 ? target + " :: " + segments[1].trim() : target;
    }

    static String write(Object value) {
        if (value instanceof String text) {
            return quote(text);
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty() ? "{}"
                    : map.entrySet().stream()
                            .map(it -> quote(String.valueOf(it.getKey())) + " : " + write(it.getValue()))
                            .collect(Collectors.joining(", ", "{ ", " }"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty() ? "[]"
                    : collection.stream().map(ApplicationModulesJson::write).collect(Collectors.joining(", ", "[ ", " ]"));
        }
        return String.valueOf(value);
    }

    /**
     * @return the text as a JSON string, with quotes, backslashes and control characters escaped
     */
    private static String quote(String text) {
        StringBuilder quoted = new StringBuilder(text.length() + 2).append('"');
        text.chars().forEach(it -> {
            if (it == '"' || it == '\\') {
                quoted.append('\\').append((char) it);
            } else if (it < FIRST_PRINTABLE) {
                quoted.append("\\u%04X".formatted(it));
            } else {
                quoted.append((char) it);
            }
        });
        return quoted.append('"').toString();
    }
}
