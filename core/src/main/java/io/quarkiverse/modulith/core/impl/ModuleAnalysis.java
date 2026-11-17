package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModuleInformation;
import io.quarkiverse.modulith.core.ApplicationModuleSource;
import io.quarkiverse.modulith.core.ArchitecturallyEvidentType;
import io.quarkiverse.modulith.core.JavaPackage;
import io.quarkiverse.modulith.core.JavaPackages;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.NamedInterfaceData;
import io.quarkiverse.modulith.core.NamedInterfaces;
import io.quarkiverse.modulith.core.PackageName;
import io.quarkiverse.modulith.core.data.DependencyData;
import io.quarkiverse.modulith.core.data.EventTypeData;
import io.quarkiverse.modulith.core.data.TypeData;

/**
 * Analyses one detected module: its declaration, named interfaces, beans, stereotypes, dependencies, events and entry
 * points. Records the stereotypes of the module's types in the shared type map.
 */
final class ModuleAnalysis {

    private final Map<String, ScannedClass> scanned;
    private final TypeFacts facts;
    private final DependencyAnalysis dependencies;
    private final EventAnalysis events;
    private final Map<String, TypeData> types;
    private final Map<String, String> moduleOfType;
    private final JavaPackages exclusions;

    ModuleAnalysis(Map<String, ScannedClass> scanned, TypeFacts facts, DependencyAnalysis dependencies,
            EventAnalysis events, Map<String, TypeData> types, Map<String, String> moduleOfType, JavaPackages exclusions) {
        this.scanned = scanned;
        this.facts = facts;
        this.dependencies = dependencies;
        this.events = events;
        this.types = types;
        this.moduleOfType = moduleOfType;
        this.exclusions = exclusions;
    }

    ModuleData module(ModuleSkeleton skeleton) {
        ApplicationModuleSource source = skeleton.source();
        JavaPackage basePackage = skeleton.basePackage();
        String id = source.getIdentifier().toString();
        ApplicationModuleInformation information = ApplicationModuleInformation.of(basePackage);
        NamedInterfaces namedInterfaces = source.getNamedInterfaces(information, basePackage);
        List<ScannedClass> classes = basePackage.getTypes().stream().map(it -> scanned.get(it.getName())).toList();
        List<String> beans = Stream.concat(classes.stream().filter(facts::isBean).map(ScannedClass::name),
                classes.stream().flatMap(facts::producedTypes)).distinct().sorted().toList();
        List<ScannedClass> repositories = classes.stream()
                .filter(it -> facts.isAssignableTo(it, Names.REPOSITORY_TYPE)).toList();
        classes.forEach(it -> types.put(it.name(), facts.classified(types.get(it.name()), it, beans, repositories)));
        List<DependencyData> references = classes.stream()
                .flatMap(it -> dependencies.dependencies(id, it, has(it, ArchitecturallyEvidentType.INJECTABLE),
                        has(it, ArchitecturallyEvidentType.CONFIGURATION_PROPERTIES)))
                .distinct().toList();
        List<EventTypeData> published = events.published(classes);
        List<String> declared = information.getDeclaredDependencies();
        boolean restricted = !declared.equals(List.of(ApplicationModuleInformation.OPEN_TOKEN));
        PackageName name = PackageName.of(basePackage.getName());
        return new ModuleData(id,
                information.getDisplayName().orElseGet(() -> ModelAnalyzer.capitalize(basePackage.getLocalName())),
                basePackage.getName(), information.isOpen(), restricted, restricted ? declared : List.of(),
                namedInterfaces.stream().map(it -> new NamedInterfaceData(it.getName(), it.getPackages(),
                        it.asJavaTypes().map(JavaType::getName).toList())).toList(),
                references.stream().map(it -> moduleOfType.get(it.target())).distinct().sorted().toList(),
                published.stream().map(EventTypeData::type).sorted().toList(),
                events.listened(classes, facts),
                classes.stream().map(ScannedClass::name).toList(),
                exclusions.stream().filter(it -> PackageName.of(it.getName()).isSubPackageOf(name))
                        .map(JavaPackage::getName).toList(),
                references, beans, published, aggregateRoots(classes),
                classes.stream().filter(it -> has(it, ArchitecturallyEvidentType.VALUE_OBJECT)).map(ScannedClass::name)
                        .toList(),
                classes.stream().flatMap(it -> facts.entryPoints(it).stream()).toList());
    }

    private boolean has(ScannedClass type, String stereotype) {
        return types.get(type.name()).stereotypes().contains(stereotype);
    }

    /**
     * @return the aggregate roots and their superclasses within the module
     */
    private List<String> aggregateRoots(List<ScannedClass> classes) {
        Set<String> moduleTypes = classes.stream().map(ScannedClass::name).collect(Collectors.toSet());
        return classes.stream()
                .filter(it -> has(it, ArchitecturallyEvidentType.AGGREGATE_ROOT))
                .flatMap(it -> Stream.concat(types.get(it.name()).supertypes().stream().filter(moduleTypes::contains),
                        Stream.of(it.name())))
                .distinct().toList();
    }
}
