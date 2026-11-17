package io.quarkiverse.modulith.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.AllowedDependency;
import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.core.ModuleBean;

/**
 * Exports the module model as JSON in Spring Modulith's {@code application-modules.json} format. Quarkus Modulith
 * writes the full form to {@code META-INF/quarkus-modulith/application-modules.json} at build time.
 */
public final class ApplicationModulesExporter {

    /**
     * Where the build puts the exported model in the application.
     */
    public static final String DEFAULT_LOCATION = "META-INF/quarkus-modulith/application-modules.json";

    private static final List<String> INITIALIZERS = List.of("io.quarkiverse.modulith.ApplicationModuleInitializer",
            "org.springframework.modulith.ApplicationModuleInitializer");

    static final String USAGE = "Usage: ApplicationModulesExporter <base package> [output file]";

    private final ApplicationModules modules;

    public ApplicationModulesExporter(ApplicationModules modules) {
        this.modules = modules;
    }

    /**
     * Writes the full JSON of the modules of a package, to the file named by the second argument if there is one,
     * otherwise to standard output.
     *
     * @param args the base package, and optionally the output file
     * @throws IllegalArgumentException with the usage if no base package is given
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException(USAGE);
        }
        String output = new ApplicationModulesExporter(ApplicationModules.of(args[0])).toFullJson();
        if (args.length == 1) {
            System.out.println(output);
        } else {
            Files.writeString(Path.of(args[1]), output);
        }
    }

    /**
     * @return the modules with their names, base package, nesting, type, allowed and actual dependencies
     */
    public String toJson() {
        return Json.toString(toMap(false));
    }

    /**
     * @return {@link #toJson()} plus the named interfaces and the initializers of each module
     */
    public String toFullJson() {
        return Json.toString(toMap(true));
    }

    private Map<String, Object> toMap(boolean full) {
        Map<String, Object> result = new LinkedHashMap<>();
        modules.forEach(it -> result.put(it.getName(), toInfo(it, full)));
        return result;
    }

    private Map<String, Object> toInfo(ApplicationModule module, boolean full) {
        List<ApplicationModule> shared = modules.getSharedModules();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("displayName", module.getDisplayName());
        json.put("basePackage", module.getBasePackage());
        modules.getParentOf(module).ifPresent(it -> json.put("parent", it.getName()));
        json.put("nested", modules.getNestedModules(module).stream().map(ApplicationModule::getName).toList());
        json.put("type", module.isOpen() ? "open" : "closed");
        json.put("shared", shared.contains(module));
        if (full) {
            Map<String, Object> namedInterfaces = new TreeMap<>();
            module.getNamedInterfaceDetails().forEach(it -> namedInterfaces.put(it.getName(),
                    it.asJavaTypes().map(JavaType::getName).collect(Collectors.toCollection(TreeSet::new))));
            json.put("namedInterfaces", namedInterfaces);
            json.put("initializers", module.getBeans().stream().map(ModuleBean::getType)
                    .filter(type -> INITIALIZERS.stream().anyMatch(type::isAssignableTo))
                    .map(JavaType::getName).toList());
        }
        List<String> allowed = allowedDependencies(module, shared);
        if (!allowed.isEmpty()) {
            json.put("allowedDependencies", allowed);
        }
        Map<String, Set<DependencyType>> dependencies = new TreeMap<>();
        module.getDirectDependencies(modules).stream().forEach(it -> dependencies
                .computeIfAbsent(it.getTargetModule().getName(), key -> EnumSet.noneOf(DependencyType.class))
                .add(it.getDependencyType()));
        json.put("dependencies", dependencies.entrySet().stream()
                .map(it -> dependency(it.getKey(), withoutDefaultIfOthersPresent(it.getValue())))
                .toList());
        return json;
    }

    private static Map<String, Object> dependency(String target, Set<DependencyType> types) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("target", target);
        json.put("types", types);
        return json;
    }

    private List<String> allowedDependencies(ApplicationModule module, List<ApplicationModule> shared) {
        try {
            return module.getAllowedDependencies(modules).stream()
                    .filter(it -> !shared.contains(it.getTargetModule()))
                    .map(AllowedDependency::toString)
                    .toList();
        } catch (IllegalArgumentException e) {
            return module.getAllowedDependencies().orElse(List.of());
        }
    }

    private static Set<DependencyType> withoutDefaultIfOthersPresent(Set<DependencyType> types) {
        if (types.stream().anyMatch(it -> it != DependencyType.DEFAULT)) {
            types.remove(DependencyType.DEFAULT);
        }
        return types;
    }
}
