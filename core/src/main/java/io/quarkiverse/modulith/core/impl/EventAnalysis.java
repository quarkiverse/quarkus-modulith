package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.data.EventSourceData;
import io.quarkiverse.modulith.core.data.EventTypeData;

/**
 * Finds the events a module publishes and listens to.
 */
final class EventAnalysis {

    private final TypePool pool;

    EventAnalysis(TypePool pool) {
        this.pool = pool;
    }

    /**
     * @return the jMolecules domain events declared in the module and the application types the module fires through
     *         an injected {@code Event}, with the places that create them
     */
    List<EventTypeData> published(List<ScannedClass> classes) {
        Stream<String> domainEvents = classes.stream().filter(this::isDomainEvent).map(ScannedClass::name);
        Stream<String> fired = classes.stream().flatMap(type -> Stream.concat(
                type.fields().stream().filter(it -> it.type().equals(Names.EVENT)).flatMap(it -> it.typeArguments().stream()),
                type.methods().stream().flatMap(method -> IntStream.range(0, method.parameterTypes().size())
                        .filter(index -> method.parameterTypes().get(index).equals(Names.EVENT))
                        .boxed()
                        .flatMap(index -> method.parameterTypeArguments().get(index).stream()))));
        return Stream.concat(domainEvents, fired.filter(this::isApplicationType))
                .distinct().sorted()
                .map(it -> new EventTypeData(it, sources(it)))
                .toList();
    }

    private boolean isDomainEvent(ScannedClass type) {
        return pool.isAssignableTo(type.name(), Names.DOMAIN_EVENT_TYPE)
                || Stream.concat(Stream.of(type.name()), pool.supertypes(type.name()).stream())
                        .flatMap(it -> pool.get(it).stream())
                        .anyMatch(it -> pool.isMetaAnnotated(it.annotations(), Names.DOMAIN_EVENT));
    }

    /**
     * @return the constructor and static factory method calls creating the event outside its own type
     */
    private List<EventSourceData> sources(String event) {
        Set<String> factories = pool.application(event).stream()
                .flatMap(it -> it.methods().stream())
                .filter(it -> it.isStatic() && it.returnType().equals(event))
                .map(it -> it.name() + it.descriptor())
                .collect(Collectors.toSet());
        return pool.application().stream()
                .filter(it -> !it.name().equals(event))
                .flatMap(type -> type.calls().stream()
                        .filter(call -> call.owner().equals(event))
                        .filter(call -> call.name().equals("<init>") || factories.contains(call.name() + call.descriptor()))
                        .map(call -> new EventSourceData(type.name(), call.originMethod(), call.originHasParameters())))
                .distinct()
                .toList();
    }

    /**
     * @return the application types the module's listener methods receive
     */
    List<String> listened(List<ScannedClass> classes, TypeFacts facts) {
        return classes.stream()
                .flatMap(type -> type.methods().stream().filter(facts::isListenerMethod))
                .flatMap(it -> it.parameterTypes().stream())
                .filter(this::isApplicationType)
                .distinct().sorted()
                .toList();
    }

    private boolean isApplicationType(String name) {
        return pool.isApplicationClass(name);
    }
}
