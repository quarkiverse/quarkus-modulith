package io.quarkiverse.modulith.deployment.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.logging.Logger;

/**
 * Finds listener methods annotated with {@code @ApplicationModuleListener} directly or through composed annotations
 * (annotation types carrying it, at any depth), like Spring's meta-annotation support.
 */
public final class ListenerDeclarations {

    public static final DotName APPLICATION_MODULE_LISTENER = DotName
            .createSimple("io.quarkiverse.modulith.ApplicationModuleListener");
    static final DotName OBSERVES = DotName.createSimple("jakarta.enterprise.event.Observes");
    static final DotName PRIORITY = DotName.createSimple("jakarta.annotation.Priority");
    static final DotName QUALIFIER = DotName.createSimple("jakarta.inject.Qualifier");
    static final DotName NORMAL_SCOPE = DotName.createSimple("jakarta.enterprise.context.NormalScope");
    static final DotName SCOPE = DotName.createSimple("jakarta.inject.Scope");
    static final Set<DotName> SERVED_SCOPES = Set.of(DotName.createSimple("jakarta.inject.Singleton"),
            DotName.createSimple("jakarta.enterprise.context.ApplicationScoped"),
            DotName.createSimple("jakarta.enterprise.context.Dependent"),
            DotName.createSimple("jakarta.enterprise.context.RequestScoped"));
    static final Set<DotName> UNSERVED_SCOPES = Set.of(DotName.createSimple("jakarta.enterprise.context.SessionScoped"),
            DotName.createSimple("jakarta.enterprise.context.ConversationScoped"));
    private static final String AFTER_SUCCESS = "AFTER_SUCCESS";
    private static final String IF_EXISTS = "IF_EXISTS";
    private static final Logger LOG = Logger.getLogger(ListenerDeclarations.class);

    private ListenerDeclarations() {
    }

    /**
     * @return each listener annotation type, {@code @ApplicationModuleListener} first, mapped to the
     *         {@code @ApplicationModuleListener} instance whose attributes it carries ({@code null} for the annotation
     *         itself)
     */
    public static Map<DotName, AnnotationInstance> annotationTypes(IndexView index) {
        Map<DotName, AnnotationInstance> types = new LinkedHashMap<>();
        types.put(APPLICATION_MODULE_LISTENER, null);
        Deque<DotName> pending = new ArrayDeque<>(List.of(APPLICATION_MODULE_LISTENER));
        while (!pending.isEmpty()) {
            DotName current = pending.poll();
            for (AnnotationInstance usage : index.getAnnotations(current)) {
                if (usage.target().kind() != AnnotationTarget.Kind.CLASS) {
                    continue;
                }
                ClassInfo annotationType = usage.target().asClass();
                if (!types.containsKey(annotationType.name())) {
                    AnnotationInstance inherited = types.get(current);
                    types.put(annotationType.name(), Objects.isNull(inherited) ? usage : inherited);
                    pending.add(annotationType.name());
                }
            }
        }
        return types;
    }

    /**
     * @param applicationClass whether a class belongs to the application archive; observers of libraries are never
     *        routed
     * @param extensionQualifiers the qualifiers extensions register without {@code @Qualifier} on the annotation type
     * @return the observer methods of application classes with the single parameter
     *         {@code @Observes(during = AFTER_SUCCESS)}, restricted to those carrying the trigger annotation when one is
     *         given. An observer keeps its plain CDI delivery when it is qualified, since the dispatcher routes by type
     *         only, when it is notified only if its bean instance exists ({@code notifyObserver = IF_EXISTS}), since the
     *         dispatcher would create the instance, and when its bean has a scope the dispatcher does not serve, such
     *         as {@code @SessionScoped}, which is not active on the thread the registry delivers on.
     */
    public static List<ListenerDeclaration> routedObservers(IndexView index, Optional<DotName> trigger,
            Predicate<DotName> applicationClass, Set<DotName> extensionQualifiers) {
        List<ListenerDeclaration> declarations = new ArrayList<>();
        for (AnnotationInstance observes : index.getAnnotations(OBSERVES)) {
            AnnotationValue during = observes.value("during");
            if (Objects.isNull(during) || !during.asEnum().equals(AFTER_SUCCESS)) {
                continue;
            }
            MethodParameterInfo parameter = observes.target().asMethodParameter();
            MethodInfo method = parameter.method();
            if (method.parametersCount() != 1 || !applicationClass.test(method.declaringClass().name())
                    || !trigger.map(method::hasAnnotation).orElse(true)) {
                continue;
            }
            Optional<String> kept = plainDeliveryReason(index, observes, extensionQualifiers);
            if (kept.isPresent()) {
                LOG.debugf("The AFTER_SUCCESS observer %s#%s keeps its CDI delivery without event publication: %s",
                        method.declaringClass().name(), method.name(), kept.get());
                continue;
            }
            LOG.debugf("Routing the AFTER_SUCCESS observer %s#%s through the event publication registry",
                    method.declaringClass().name(), method.name());
            declarations.add(new ListenerDeclaration(method, null, true));
        }
        return declarations;
    }

    /**
     * @return why the observer cannot be routed, empty when it can
     */
    static Optional<String> plainDeliveryReason(IndexView index, AnnotationInstance observes,
            Set<DotName> extensionQualifiers) {
        AnnotationValue reception = observes.value("notifyObserver");
        if (Objects.nonNull(reception) && reception.asEnum().equals(IF_EXISTS)) {
            return Optional.of("it is notified only if its bean instance exists (notifyObserver = IF_EXISTS)");
        }
        if (qualified(index, observes.target().asMethodParameter(), extensionQualifiers)) {
            return Optional.of("it is qualified, and the registry routes events by type only");
        }
        return unservedScope(index, observes.target().asMethodParameter().method().declaringClass())
                .map(it -> "its bean has the scope @" + it.withoutPackagePrefix()
                        + ", which is not active when the registry delivers");
    }

    /**
     * @return the scope the bean class declares when the dispatcher cannot serve it: a scope other than
     *         {@code @Singleton}, {@code @ApplicationScoped}, {@code @Dependent} and {@code @RequestScoped}, which the
     *         dispatcher activates
     */
    static Optional<DotName> unservedScope(IndexView index, ClassInfo bean) {
        return bean.declaredAnnotations().stream().map(AnnotationInstance::name)
                .filter(it -> !SERVED_SCOPES.contains(it))
                .filter(it -> UNSERVED_SCOPES.contains(it) || isScope(index, it))
                .findFirst();
    }

    private static boolean isScope(IndexView index, DotName annotation) {
        return Optional.ofNullable(index.getClassByName(annotation))
                .filter(it -> it.hasDeclaredAnnotation(NORMAL_SCOPE) || it.hasDeclaredAnnotation(SCOPE))
                .isPresent();
    }

    /**
     * An annotation counts as a qualifier unless the index shows it is not one, except {@code @Priority}, which only
     * orders observers. Qualifiers registered by extensions count too.
     */
    static boolean qualified(IndexView index, MethodParameterInfo parameter, Set<DotName> extensionQualifiers) {
        for (AnnotationInstance annotation : parameter.declaredAnnotations()) {
            DotName name = annotation.name();
            if (name.equals(OBSERVES) || name.equals(PRIORITY)) {
                continue;
            }
            ClassInfo type = index.getClassByName(name);
            if (Objects.isNull(type)) {
                LOG.debugf("@%s on the observer %s#%s is not in the index, so it counts as a qualifier", name,
                        parameter.method().declaringClass().name(), parameter.method().name());
                return true;
            }
            if (type.hasDeclaredAnnotation(QUALIFIER) || extensionQualifiers.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the listener methods, in index order per annotation type
     */
    public static List<ListenerDeclaration> find(IndexView index) {
        List<ListenerDeclaration> declarations = new ArrayList<>();
        for (Map.Entry<DotName, AnnotationInstance> type : annotationTypes(index).entrySet()) {
            for (AnnotationInstance usage : index.getAnnotations(type.getKey())) {
                if (usage.target().kind() == AnnotationTarget.Kind.METHOD) {
                    declarations.add(new ListenerDeclaration(usage.target().asMethod(),
                            Objects.requireNonNullElse(type.getValue(), usage), false));
                }
            }
        }
        return declarations;
    }
}
