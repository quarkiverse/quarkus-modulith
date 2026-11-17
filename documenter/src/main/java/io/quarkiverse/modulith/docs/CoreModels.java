package io.quarkiverse.modulith.docs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ArchitecturallyEvidentType;
import io.quarkiverse.modulith.core.EventType;
import io.quarkiverse.modulith.core.JavaType;

/**
 * Derives the {@link ModuleDetails} of a module from the core module model, the way Spring Modulith's documenter reads
 * its {@code ApplicationModules}: types, beans with their stereotypes and listener methods, typed dependencies,
 * published events with their sources, aggregate roots, value types, named interfaces and initializers. The build
 * takes the dependencies and events from here too, and adds what the Jandex index tells.
 */
public final class CoreModels {

    private static final String PACKAGE_INFO = ".package-info";

    private CoreModels() {
    }

    static ModuleDetails details(ApplicationModules modules, ApplicationModule module, ConfigProperties configProperties) {
        List<ModuleType> types = module.getJavaPackage().stream().map(it -> type(module, it)).toList();
        List<ModuleProperty> properties = module.getJavaPackage().stream()
                .filter(it -> !it.isPackageInfo())
                .flatMap(it -> configProperties.of(it).stream())
                .sorted(Comparator.comparing(ModuleProperty::name))
                .toList();
        List<ModuleBean> beans = module.getBeans().stream().map(it -> bean(module, it)).toList();
        List<ModuleDependency> dependencies = dependencies(modules, module);
        List<PublishedEvent> events = events(module);
        Map<String, List<String>> namedInterfaces = new LinkedHashMap<>();
        module.getNamedInterfaceDetails().forEach(it -> namedInterfaces.put(it.getName(),
                it.asJavaTypes().map(JavaType::getName).toList()));
        List<String> initializers = module.getBeans(ApplicationModuleInitializer.class).stream()
                .map(it -> it.getType().getName()).toList();
        return new ModuleDetails(module.getName(), modules.getParentOf(module).map(ApplicationModule::getName), types,
                beans, dependencies, events, module.getAggregateRoots().stream().map(it -> type(module, it)).toList(),
                module.getValueTypes().stream().map(it -> type(module, it)).toList(), properties, namedInterfaces,
                initializers);
    }

    /**
     * @return the direct dependencies of the module with their types, ordered by target module, type, source and target
     */
    public static List<ModuleDependency> dependencies(ApplicationModules modules, ApplicationModule module) {
        return module.getDirectDependencies(modules).stream()
                .map(it -> new ModuleDependency(it.getSourceType(), it.getTargetType(), it.getDependencyType(),
                        it.getTargetModule().getName()))
                .sorted(Comparator.comparing(ModuleDependency::targetModule).thenComparing(ModuleDependency::type)
                        .thenComparing(ModuleDependency::sourceType).thenComparing(ModuleDependency::targetType))
                .toList();
    }

    /**
     * @return the events the module publishes, with the places creating them
     */
    public static List<PublishedEvent> events(ApplicationModule module) {
        return module.getPublishedEventTypes().stream().map(it -> event(module, it)).toList();
    }

    static ModuleType type(ApplicationModule module, JavaType type) {
        Set<String> packageAnnotations = new LinkedHashSet<>();
        module.getType(type.getPackageName() + PACKAGE_INFO).ifPresent(info -> {
            packageAnnotations.addAll(info.getAnnotationNames());
            packageAnnotations.addAll(info.getMetaAnnotationNames());
        });
        return new ModuleType(type.getName(), type.isPublic(), module.isExposed(type),
                Set.copyOf(type.getAnnotationNames()), Set.copyOf(type.getMetaAnnotationNames()), packageAnnotations,
                Set.copyOf(type.getSupertypeNames()));
    }

    private static ModuleBean bean(ApplicationModule module, io.quarkiverse.modulith.core.ModuleBean bean) {
        ArchitecturallyEvidentType evident = bean.toArchitecturallyEvidentType();
        Set<Stereotype> stereotypes = EnumSet.noneOf(Stereotype.class);
        if (evident.isController()) {
            stereotypes.add(Stereotype.CONTROLLER);
        }
        if (evident.isService()) {
            stereotypes.add(Stereotype.SERVICE);
        }
        if (evident.isRepository()) {
            stereotypes.add(Stereotype.REPOSITORY);
        }
        if (evident.isEventListener()) {
            stereotypes.add(Stereotype.EVENT_LISTENER);
        }
        if (evident.isConfigurationProperties()) {
            stereotypes.add(Stereotype.CONFIGURATION_PROPERTIES);
        }
        List<ListenerMethod> methods = evident.getReferenceMethods()
                .map(it -> new ListenerMethod(it.getName(), it.getParameterTypeNames(), it.getReferenceTypeNames(),
                        it.isAsync()))
                .sorted(Comparator.comparing(ListenerMethod::signature))
                .toList();
        return new ModuleBean(type(module, bean.getType()), bean.isApiBean(),
                bean.getInterfacesWithinModule().stream().map(it -> type(module, it)).toList(), stereotypes, methods);
    }

    /**
     * Reads the sources from their rendering, {@code c.a.o.OrderManagement.complete(…)}, as Spring Modulith's
     * documenter does.
     */
    private static PublishedEvent event(ApplicationModule module, EventType event) {
        List<EventSource> sources = new ArrayList<>();
        event.getSources().forEach(source -> {
            String text = source.toString(module);
            String call = text.substring(0, text.indexOf('('));
            int dot = call.lastIndexOf('.');
            sources.add(new EventSource(call.substring(0, dot), call.substring(dot + 1), text.endsWith("(…)")));
        });
        return new PublishedEvent(type(module, event.getType()), sources);
    }
}
