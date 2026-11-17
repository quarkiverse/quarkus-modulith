package io.quarkiverse.modulith.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.Violation.Kind;

/**
 * Checks the module rules of Spring Modulith:
 * <ol>
 * <li>module declarations name existing modules and named interfaces,</li>
 * <li>closed modules do not form dependency cycles,</li>
 * <li>modules that declare allowed dependencies only depend on what they declare,</li>
 * <li>modules only reference exposed types of other modules,</li>
 * <li>nested modules are only reached from their parent and its other nested modules,</li>
 * <li>beans of other modules are not injected into fields.</li>
 * </ol>
 * The messages are the ones Spring Modulith 2.1.2 uses for the same breach.
 */
final class ModuleVerifier {

    static final String INTERNAL_REFERENCE = "Module '%s' depends on non-exposed type %s within module '%s'!";
    static final String INVALID_SUB_MODULE_REFERENCE = "Invalid sub-module reference from module '%s' to module '%s' (via %s -> %s)!";
    static final String NOT_ALLOWED = "Module '%s' depends on %s via %s -> %s. Allowed targets: %s.";
    static final String FIELD_INJECTION = "Module %s uses field injection in %s. Prefer constructor injection instead!";
    static final String CYCLES_OMITTED = "Cycle detection stopped after %d cycles, further cycles are omitted."
            + " Break the cycles above and verify again.";

    /**
     * The most cycles reported, ArchUnit's default for {@code cycles.maxNumberToDetect}. The number of elementary
     * cycles grows exponentially with the number of tightly coupled modules.
     */
    static final int MAX_CYCLES = 100;

    private ModuleVerifier() {
    }

    static Violations verify(ApplicationModules modules) {
        Violations result = new Violations(invalidDeclarations(modules)).and(new Violations(cycles(modules)));
        return modules.stream().map(it -> detectDependencies(it, modules)).reduce(result, Violations::and);
    }

    static List<Violation> invalidDeclarations(ApplicationModules modules) {
        List<Violation> violations = new ArrayList<>();
        modules.getModelData().sharedModules().stream().filter(it -> modules.getModuleByName(it).isEmpty())
                .forEach(it -> violations.add(new Violation(Kind.INVALID_DECLARATION, it, "", it,
                        "@Modulith declares the shared module '" + it + "', which does not exist")));
        modules.stream().filter(it -> it.getName().contains("::")).forEach(it -> violations.add(new Violation(
                Kind.INVALID_DECLARATION, it.getName(), it.getBasePackage(), "",
                "Module identifier '" + it.getName() + "' must not contain '::'")));
        modules.stream().forEach(module -> module.getAllowedDependencies().orElse(List.of()).stream()
                .flatMap(it -> invalidDeclaration(module, it, modules).stream())
                .forEach(violations::add));
        return violations;
    }

    private static Optional<Violation> invalidDeclaration(ApplicationModule module, String declaration,
            ApplicationModules modules) {
        try {
            AllowedDependency.of(declaration, module, modules);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(new Violation(Kind.INVALID_DECLARATION, module.getName(), module.getBasePackage(),
                    declaration, e.getMessage()));
        }
    }

    /**
     * @return the declaration as grant, empty if it names an unknown module or interface, which
     *         {@link #invalidDeclarations(ApplicationModules)} reports
     */
    private static Optional<AllowedDependency> validDeclaration(ApplicationModule module, String declaration,
            ApplicationModules modules) {
        try {
            return Optional.of(AllowedDependency.of(declaration, module, modules));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static Violations detectDependencies(ApplicationModule module, ApplicationModules modules) {
        AllowedDependencies allowed = module.getAllowedDependencies(modules,
                it -> validDeclaration(module, it, modules));
        return module.qualifiedDependencies(modules)
                .flatMap(it -> check(module, it, allowed, modules))
                .reduce(Violations.NONE, Violations::and, Violations::and);
    }

    private static Stream<Violation> check(ApplicationModule origin, DefaultApplicationModuleDependency dependency,
            AllowedDependencies allowed, ApplicationModules modules) {
        Stream<Violation> fieldInjection = Optional.of(dependency.getData().injectedField()).filter(it -> !it.isEmpty())
                .map(it -> new Violation(Kind.FIELD_INJECTION, origin.getName(), it, dependency.getTargetType(),
                        FIELD_INJECTION.formatted(origin.getDisplayName(), it)))
                .stream();
        return Stream.concat(checkAccess(origin, dependency, allowed, modules).stream(), fieldInjection);
    }

    private static Optional<Violation> checkAccess(ApplicationModule origin, DefaultApplicationModuleDependency dependency,
            AllowedDependencies allowed, ApplicationModules modules) {
        ApplicationModule targetModule = dependency.getTargetModule();
        JavaType target = dependency.getTargetJavaType();
        if (!allowed.isAllowedDependency(target)) {
            boolean targetModuleAllowed = allowed.stream().anyMatch(it -> it.getTargetModule().equals(targetModule));
            if (targetModuleAllowed && !targetModule.isExposed(target)) {
                return Optional.of(nonExposed(origin, dependency));
            }
            List<String> interfaces = targetModule.getNamedInterfaceDetails().getNamedInterfacesContaining(target)
                    .filter(NamedInterface::isNamed)
                    .map(it -> it.getQualifiedName(targetModule.getIdentifier()))
                    .toList();
            String targetString = interfaces.isEmpty() ? "module '%s'".formatted(targetModule.getIdentifier())
                    : "named interface(s) '%s'".formatted(String.join(", ", interfaces));
            return Optional.of(new Violation(Kind.DEPENDENCY_NOT_ALLOWED, origin.getName(), dependency.getSourceType(),
                    dependency.getTargetType(), NOT_ALLOWED.formatted(origin.getIdentifier(), targetString,
                            dependency.getSourceType(), dependency.getTargetType(), allowed)));
        }
        if (targetModule.isOpen() || origin.containsTypeInAnyParent(target, modules)) {
            return Optional.empty();
        }
        if (!targetModule.isExposed(target)) {
            return Optional.of(nonExposed(origin, dependency));
        }
        if (!haveSameParentOrDirectParentRelationship(origin, targetModule, modules)) {
            return Optional.of(new Violation(Kind.NESTED_MODULE_ACCESS, origin.getName(), dependency.getSourceType(),
                    dependency.getTargetType(), INVALID_SUB_MODULE_REFERENCE.formatted(origin.getIdentifier(),
                            targetModule.getIdentifier(),
                            FormattableType.of(dependency.getSourceJavaType()).getAbbreviatedFullName(origin),
                            FormattableType.of(target).getAbbreviatedFullName(targetModule))));
        }
        return Optional.empty();
    }

    private static Violation nonExposed(ApplicationModule origin, DefaultApplicationModuleDependency dependency) {
        return new Violation(Kind.NON_EXPOSED_TYPE, origin.getName(), dependency.getSourceType(),
                dependency.getTargetType(), INTERNAL_REFERENCE.formatted(origin.getIdentifier(), dependency.getTargetType(),
                        dependency.getTargetModule().getIdentifier()) + System.lineSeparator() + dependency.getDescription());
    }

    private static boolean haveSameParentOrDirectParentRelationship(ApplicationModule source, ApplicationModule target,
            ApplicationModules modules) {
        Optional<ApplicationModule> sourceParent = modules.getParentOf(source);
        Optional<ApplicationModule> targetParent = modules.getParentOf(target);
        return targetParent.isEmpty()
                || targetParent.filter(source::equals).isPresent()
                || sourceParent.equals(targetParent);
    }

    /**
     * @return one violation per elementary cycle between closed modules, at most {@link #MAX_CYCLES}, followed by a
     *         note when further cycles were left out
     */
    static List<Violation> cycles(ApplicationModules modules) {
        return cycles(modules, MAX_CYCLES);
    }

    static List<Violation> cycles(ApplicationModules modules, int maxCycles) {
        Map<String, Set<String>> graph = new TreeMap<>();
        Map<String, Map<String, String>> examples = new HashMap<>();
        modules.stream().filter(it -> !it.isOpen()).forEach(module -> {
            Set<String> targets = new TreeSet<>();
            module.qualifiedDependencies(modules)
                    .filter(it -> !it.getTargetModule().isOpen())
                    .forEach(it -> {
                        targets.add(it.getTargetModule().getName());
                        examples.computeIfAbsent(module.getName(), key -> new HashMap<>())
                                .putIfAbsent(it.getTargetModule().getName(), it.getDescription());
                    });
            graph.put(module.getName(), targets);
        });
        List<List<String>> found = new ArrayList<>();
        int searched = maxCycles + 1;
        graph.keySet().stream().takeWhile(it -> found.size() < searched)
                .forEach(start -> new CycleSearch(graph, start, searched, found).run());
        List<Violation> violations = new ArrayList<>(found.stream().limit(maxCycles).map(it -> cycle(it, examples)).toList());
        if (found.size() > maxCycles) {
            violations.add(new Violation(Kind.CYCLE, "", "", "", CYCLES_OMITTED.formatted(maxCycles)));
        }
        return violations;
    }

    private static Violation cycle(List<String> members, Map<String, Map<String, String>> examples) {
        List<String> cycle = new ArrayList<>(members);
        cycle.add(members.get(0));
        StringBuilder message = new StringBuilder("Cycle detected: ").append(String.join(" -> ", cycle));
        for (int i = 0; i < cycle.size() - 1; i++) {
            message.append("\n  ").append(i + 1).append(". Dependencies of ").append(cycle.get(i)).append("\n    - ")
                    .append(examples.get(cycle.get(i)).get(cycle.get(i + 1)));
        }
        return new Violation(Kind.CYCLE, cycle.get(0), String.join(" -> ", cycle), "", message.toString());
    }
}
