package io.quarkiverse.modulith.core.par;

import java.util.stream.Stream;

import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.EventType;
import org.springframework.modulith.core.SpringBean;

import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Takes the snapshot of a model built by Spring Modulith 2.1.2.
 */
final class SpringSnapshots {

    private SpringSnapshots() {
    }

    static ModelSnapshot of(Class<?> application) {
        ApplicationModules modules = ApplicationModules.of(application, location -> true);
        return new ModelSnapshot(
                ModelSnapshot.sorted(modules.stream().map(it -> it.getIdentifier() + " | " + it.getDisplayName() + " | "
                        + it.getBasePackage().getName() + " | " + it.isOpen() + " | "
                        + modules.getParentOf(it).map(p -> p.getIdentifier().toString()).orElse("-")).toList()),
                ModelSnapshot.sorted(modules.stream().flatMap(module -> module.getNamedInterfaces().stream()
                        .map(it -> module.getIdentifier() + " :: " + it.getName() + " -> "
                                + ModelSnapshot.sorted(it.asJavaClasses().map(JavaClass::getName).toList())))
                        .toList()),
                ModelSnapshot.sorted(modules.stream().flatMap(module -> module.getDirectDependencies(modules).stream()
                        .map(it -> module.getIdentifier() + ": " + it.getSourceType().getName() + " -> "
                                + it.getTargetType().getName() + " (" + it.getDependencyType() + ")"))
                        .toList()),
                ModelSnapshot.sorted(modules.detectViolations().getMessages().stream().map(ModelSnapshot::violation)
                        .toList()),
                ModelSnapshot.sorted(modules.stream().flatMap(SpringSnapshots::details).toList()));
    }

    private static Stream<String> details(ApplicationModule module) {
        return Stream.of(
                module.getPublishedEvents().stream().map(EventType::getType).map(it -> "event " + it.getName()),
                module.getPublishedEvents().stream().flatMap(it -> it.getSources().stream()
                        .map(source -> "source " + it.getType().getName() + " <- " + source.toString(module))),
                module.getAggregateRoots().stream().map(it -> "aggregate " + it.getName()),
                module.getValueTypes().stream().map(it -> "value " + it.getName()),
                module.getSpringBeans().stream().map(SpringBean::getFullyQualifiedTypeName).map(it -> "bean " + it))
                .flatMap(it -> it)
                .map(it -> module.getIdentifier() + " " + it);
    }
}
