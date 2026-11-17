package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import jakarta.enterprise.event.Event;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Decides which event types externalization observes: the types the beans of the application fire through injected
 * {@code Event<X>}, and the externalized types. Beans of libraries and other extensions are left out. A type is left
 * out when a supertype is observed too, so that each event reaches the externalization once. An {@code Event<Object>},
 * a raw {@code Event} or a type variable makes externalization observe {@code Object}, which is logged at debug level.
 */
final class ObservedEventTypes {

    static final DotName OBJECT = DotName.createSimple(Object.class);
    private static final DotName EVENT = DotName.createSimple(Event.class);
    private static final Logger LOG = Logger.getLogger(ObservedEventTypes.class);

    private final IndexView index;
    private final Predicate<String> applicationClass;

    /**
     * @param index an index that also knows the JDK and library classes, used to walk type hierarchies
     * @param applicationClass whether a bean class, by name, belongs to the application
     */
    ObservedEventTypes(IndexView index, Predicate<String> applicationClass) {
        this.index = index;
        this.applicationClass = applicationClass;
    }

    /**
     * @return the types to observe, sorted by name
     */
    Set<DotName> collect(Collection<EventInjectionPoint> injectionPoints, Collection<String> externalizedTypes) {
        Set<DotName> fired = new HashSet<>();
        for (EventInjectionPoint injectionPoint : injectionPoints) {
            Type required = injectionPoint.requiredType();
            if (required.name().equals(EVENT) && applicationClass.test(injectionPoint.beanClass())) {
                DotName type = eventType(required);
                if (type.equals(OBJECT)) {
                    LOG.debugf("%s injects %s, so externalization observes every event", injectionPoint.beanClass(),
                            required);
                }
                fired.add(type);
            }
        }
        externalizedTypes.stream().map(DotName::createSimple).forEach(fired::add);
        if (fired.contains(OBJECT)) {
            return Set.of(OBJECT);
        }
        Set<DotName> observed = new TreeSet<>();
        for (DotName type : fired) {
            if (supertypes(type).stream().noneMatch(fired::contains)) {
                observed.add(type);
            }
        }
        return observed;
    }

    static DotName eventType(Type event) {
        if (event.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return OBJECT;
        }
        Type argument = event.asParameterizedType().arguments().get(0);
        boolean concrete = argument.kind() == Type.Kind.CLASS || argument.kind() == Type.Kind.PARAMETERIZED_TYPE;
        return concrete ? argument.name() : OBJECT;
    }

    /**
     * @return the proper supertypes of the type the index knows, without {@code Object}
     */
    Set<DotName> supertypes(DotName type) {
        Set<DotName> result = new HashSet<>();
        Deque<DotName> pending = new ArrayDeque<>(List.of(type));
        while (!pending.isEmpty()) {
            ClassInfo current = index.getClassByName(pending.poll());
            if (Objects.nonNull(current)) {
                Optional.ofNullable(current.superName()).filter(name -> !name.equals(OBJECT)).ifPresent(pending::add);
                pending.addAll(current.interfaceNames());
                if (!current.name().equals(type)) {
                    result.add(current.name());
                }
            }
        }
        return result;
    }
}
