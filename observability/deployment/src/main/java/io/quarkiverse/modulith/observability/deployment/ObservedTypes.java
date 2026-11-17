package io.quarkiverse.modulith.observability.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.ListenerMethod;
import io.quarkiverse.modulith.docs.ModuleBean;
import io.quarkiverse.modulith.docs.ModuleDetails;
import io.quarkiverse.modulith.docs.ModuleType;
import io.quarkiverse.modulith.docs.Stereotype;
import io.quarkiverse.modulith.docs.TypeNames;
import io.quarkiverse.modulith.observability.ModuleInvocation;
import io.quarkiverse.modulith.observability.runtime.ObservedModules;

/**
 * Decides at build time which beans are observed and how their methods are named, Spring Modulith's
 * {@code ObservedModuleType} and {@code DefaultObservedModule}: REST resources, event listeners of other modules'
 * events, beans whose class or a supertype is exposed, messaging endpoints, and the generated implementations of REST
 * Data with Panache resources (Spring Modulith's {@code SpringDataRestModuleObservabilityBeanPostProcessor}).
 */
final class ObservedTypes {

    static final String EVENT_LISTENER = "event-listener";
    static final String BEAN = "bean";
    static final DotName PRODUCES = DotName.createSimple("jakarta.enterprise.inject.Produces");

    private final DocumentationModel model;
    private final IndexView index;

    ObservedTypes(DocumentationModel model, IndexView index) {
        this.model = model;
        this.index = index;
    }

    ObservedTypesBuildItem compute() {
        Set<String> classes = new LinkedHashSet<>();
        Set<String> resources = new LinkedHashSet<>();
        List<String> rows = new ArrayList<>();
        for (ApplicationModule module : model.modules()) {
            ModuleDetails details = model.details(module);
            details.types().stream().filter(ObservedTypes::isRestDataResource).forEach(resource -> {
                resources.add(resource.name());
                rows.add(ObservedModules.resourceRow(resource.name(), new ModuleInvocation(module.getName(),
                        module.getDisplayName(), "", BEAN, module.getDisplayName() + " > "
                                + TypeNames.abbreviate(resource.name(), module.getBasePackage()))));
            });
            for (ModuleBean bean : details.beans()) {
                if (isObserved(module, details, bean)) {
                    classes.add(bean.type().name());
                    rows.addAll(rows(module, bean));
                }
            }
        }
        return new ObservedTypesBuildItem(classes, resources, rows);
    }

    /**
     * @return whether the type is a REST Data with Panache resource interface, observed like Spring Modulith observes
     *         Spring Data REST repositories
     */
    static boolean isRestDataResource(ModuleType type) {
        return type.supertypes().stream().anyMatch(it -> it.startsWith("io.quarkus.")
                && it.contains(".rest.data.panache.") && it.endsWith("Resource"));
    }

    boolean isObserved(ApplicationModule module, ModuleDetails details, ModuleBean bean) {
        return bean.is(Stereotype.CONTROLLER) || bean.is(Stereotype.MESSAGING_ENDPOINT)
                || listensToOtherModulesEvents(module, bean) || exposesTypeOrSupertype(details, bean.type());
    }

    /**
     * @return whether the bean has listener methods and one of the types they refer to belongs to another module or to
     *         no module, whatever the order of the methods; listener methods that refer to no type count as listening
     *         to other modules' events
     */
    private boolean listensToOtherModulesEvents(ApplicationModule module, ModuleBean bean) {
        if (bean.listenerMethods().isEmpty()) {
            return false;
        }
        List<String> types = bean.listenerMethods().stream().flatMap(it -> it.referenceTypes().stream()).toList();
        return types.isEmpty() || types.stream().anyMatch(it -> model.moduleByType(it)
                .map(owner -> !owner.getName().equals(module.getName()))
                .orElse(true));
    }

    private static boolean exposesTypeOrSupertype(ModuleDetails details, ModuleType type) {
        return type.exposed() || type.supertypes().stream()
                .anyMatch(it -> details.type(it).map(ModuleType::exposed).orElse(false));
    }

    private List<String> rows(ApplicationModule module, ModuleBean bean) {
        List<String> rows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String beanClass = bean.type().name();
        String type = TypeNames.abbreviate(beanClass, module.getBasePackage());
        for (ClassInfo current = index.getClassByName(DotName.createSimple(beanClass)); Objects.nonNull(current)
                && !current.name().toString().equals(Object.class.getName()); current = superclass(current)) {
            for (MethodInfo method : current.methods()) {
                String signature = signature(method);
                if (isBusinessMethod(method) && seen.add(signature)) {
                    String parameters = method.parameterTypes().stream()
                            .map(it -> render(it, module.getBasePackage()))
                            .collect(Collectors.joining(", ", "(", ")"));
                    ModuleInvocation invocation = new ModuleInvocation(module.getName(), module.getDisplayName(),
                            method.name(), isListener(bean, method) ? EVENT_LISTENER : BEAN,
                            module.getDisplayName() + " > " + type + "." + method.name() + parameters);
                    rows.add(ObservedModules.row(beanClass, signature, invocation));
                }
            }
        }
        return rows;
    }

    private ClassInfo superclass(ClassInfo type) {
        return Optional.ofNullable(type.superName()).map(index::getClassByName).orElse(null);
    }

    /**
     * @return whether calls of the method are observed: not constructors, private, static, synthetic or producer
     *         methods, which are framework calls rather than calls into the module
     */
    private static boolean isBusinessMethod(MethodInfo method) {
        int flags = method.flags();
        return !method.isConstructor() && !method.isStaticInitializer() && !Modifier.isPrivate(flags)
                && !Modifier.isStatic(flags) && !method.isSynthetic() && !method.hasDeclaredAnnotation(PRODUCES);
    }

    private static boolean isListener(ModuleBean bean, MethodInfo method) {
        List<String> parameters = method.parameterTypes().stream().map(ObservedTypes::binaryName).toList();
        return bean.listenerMethods().stream().map(ListenerMethod::signature)
                .anyMatch(new ListenerMethod(method.name(), parameters, List.of(), false).signature()::equals);
    }

    /**
     * @return the method name and the binary names of the erased parameter types, as the runtime computes it from a
     *         {@code java.lang.reflect.Method}
     */
    static String signature(MethodInfo method) {
        return method.name() + method.parameterTypes().stream().map(ObservedTypes::binaryName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    static String binaryName(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            return binaryName(type.asArrayType().elementType()) + "[]".repeat(type.asArrayType().deepDimensions());
        }
        return type.name().toString();
    }

    /**
     * @return the type abbreviated like Spring Modulith renders parameters: module types relative to the module, other
     *         types abbreviated, type arguments included
     */
    static String render(Type type, String basePackage) {
        return switch (type.kind()) {
            case ARRAY -> render(type.asArrayType().elementType(), basePackage)
                    + "[]".repeat(type.asArrayType().deepDimensions());
            case PARAMETERIZED_TYPE -> TypeNames.abbreviate(type.name().toString(), basePackage, true)
                    + type.asParameterizedType().arguments().stream().map(it -> render(it, basePackage))
                            .collect(Collectors.joining(", ", "<", ">"));
            case WILDCARD_TYPE, TYPE_VARIABLE, UNRESOLVED_TYPE_VARIABLE, TYPE_VARIABLE_REFERENCE -> "?";
            default -> TypeNames.abbreviate(type.name().toString(), basePackage, true);
        };
    }
}
