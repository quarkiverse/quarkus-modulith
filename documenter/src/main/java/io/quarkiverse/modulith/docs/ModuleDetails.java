package io.quarkiverse.modulith.docs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the documentation knows about one application module beyond the module model itself: its types, beans, typed
 * dependencies, events and configuration properties.
 *
 * @param identifier the module identifier
 * @param parent the identifier of the enclosing module, for a nested module
 * @param types the types of the module
 * @param beans the beans of the module
 * @param dependencies the references from the module's types to types of other modules
 * @param publishedEvents the events the module publishes
 * @param aggregateRoots the aggregate roots of the module
 * @param valueTypes the value types of the module
 * @param properties the configuration properties the module declares
 * @param namedInterfaces the named interfaces of the module, in declaration order, with the binary names of the types
 *        they expose
 * @param initializers the binary names of the module's {@code ApplicationModuleInitializer} beans
 */
public record ModuleDetails(String identifier, Optional<String> parent, List<ModuleType> types, List<ModuleBean> beans,
        List<ModuleDependency> dependencies, List<PublishedEvent> publishedEvents, List<ModuleType> aggregateRoots,
        List<ModuleType> valueTypes, List<ModuleProperty> properties, Map<String, List<String>> namedInterfaces,
        List<String> initializers) {

    public ModuleDetails {
        types = List.copyOf(types);
        beans = List.copyOf(beans);
        dependencies = List.copyOf(dependencies);
        publishedEvents = List.copyOf(publishedEvents);
        aggregateRoots = List.copyOf(aggregateRoots);
        valueTypes = List.copyOf(valueTypes);
        properties = List.copyOf(properties);
        namedInterfaces = Collections.unmodifiableMap(new LinkedHashMap<>(namedInterfaces));
        initializers = List.copyOf(initializers);
    }

    /**
     * @return details that know nothing about the module beyond its identifier
     */
    public static ModuleDetails of(String identifier) {
        return new ModuleDetails(identifier, Optional.empty(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), Map.of(), List.of());
    }

    /**
     * @return the type of the module with the given binary name
     */
    public Optional<ModuleType> type(String name) {
        return types.stream().filter(it -> it.name().equals(name)).findFirst();
    }
}
