package io.quarkiverse.modulith.core.par;

import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.EventType;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.core.ModuleBean;

/**
 * Takes the snapshot of a model built by Quarkus Modulith.
 */
final class QuarkusSnapshots {

    private QuarkusSnapshots() {
    }

    static ModelSnapshot of(Class<?> application) {
        ApplicationModules modules = ApplicationModules.of(application, ClassLocations.ALL);
        return new ModelSnapshot(
                ModelSnapshot.sorted(modules.stream().map(it -> it.getIdentifier() + " | " + it.getDisplayName() + " | "
                        + it.getBasePackage() + " | " + it.isOpen() + " | "
                        + modules.getParentOf(it).map(p -> p.getIdentifier().toString()).orElse("-")).toList()),
                ModelSnapshot.sorted(modules.stream().flatMap(module -> module.getNamedInterfaceDetails().stream()
                        .map(it -> module.getIdentifier() + " :: " + it.getName() + " -> "
                                + ModelSnapshot.sorted(it.asJavaTypes().map(JavaType::getName).toList())))
                        .toList()),
                ModelSnapshot.sorted(modules.stream().flatMap(module -> module.getDirectDependencies(modules).stream()
                        .map(it -> module.getIdentifier() + ": " + it.getSourceType() + " -> " + it.getTargetType() + " ("
                                + it.getDependencyType() + ")"))
                        .toList()),
                ModelSnapshot.sorted(modules.detectViolations().getMessages().stream().map(ModelSnapshot::violation)
                        .toList()),
                ModelSnapshot.sorted(modules.stream().flatMap(QuarkusSnapshots::details).toList()));
    }

    private static Stream<String> details(ApplicationModule module) {
        return Stream.of(
                module.getPublishedEventTypes().stream().map(EventType::getType).map(it -> "event " + it.getName()),
                module.getPublishedEventTypes().stream().flatMap(it -> it.getSources().stream()
                        .map(source -> "source " + it.getType().getName() + " <- " + source.toString(module))),
                module.getAggregateRoots().stream().map(it -> "aggregate " + it.getName()),
                module.getValueTypes().stream().map(it -> "value " + it.getName()),
                module.getBeans().stream().map(ModuleBean::getFullyQualifiedTypeName).map(it -> "bean " + it))
                .flatMap(it -> it)
                .map(it -> module.getIdentifier() + " " + it);
    }
}
