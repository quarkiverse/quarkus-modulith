package io.quarkiverse.modulith.deployment;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional.TxType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.deployment.events.ConditionCompiler;
import io.quarkiverse.modulith.deployment.events.ConditionNode;
import io.quarkiverse.modulith.deployment.events.ConditionParser;
import io.quarkiverse.modulith.deployment.events.ListenerDeclaration;
import io.quarkiverse.modulith.deployment.events.ListenerDeclarations;
import io.quarkiverse.modulith.deployment.events.ListenerDeclarationsBuildItem;
import io.quarkiverse.modulith.deployment.events.RegistryRoutingBuildItem;
import io.quarkiverse.modulith.deployment.events.RoutedObserverVeto;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.moduletest.ExcludedModulesBuildItem;
import io.quarkiverse.modulith.runtime.events.JtaTransactionSupport;
import io.quarkiverse.modulith.runtime.events.ListenerBuildTimeConfig;
import io.quarkiverse.modulith.runtime.events.ListenerData;
import io.quarkiverse.modulith.runtime.events.ListenerExecutorKind;
import io.quarkiverse.modulith.runtime.events.ListenerInvokerMaps;
import io.quarkiverse.modulith.runtime.events.ListenerInvokers;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkiverse.modulith.runtime.events.ModuleEventObservers;
import io.quarkiverse.modulith.runtime.events.NoTransactionSupport;
import io.quarkiverse.modulith.runtime.events.VirtualThreadListenerExecutor;
import io.quarkiverse.modulith.runtime.events.WorkerListenerExecutor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.InvokerFactoryBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.deployment.ObserverTransformerBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.InvokerInfo;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Finds the {@code @ApplicationModuleListener} methods, validates them, creates their invokers and compiles their
 * conditions, and registers one synthetic observer per listened event type. The listener table is handed to the
 * dispatcher by {@code ListenerRecordingSteps}.
 */
class ListenerProcessor {

    static final DotName CONDITIONAL_EVENT_LISTENER = DotName
            .createSimple("io.quarkiverse.modulith.events.ConditionalEventListener");
    private static final DotName DISPATCHER = DotName.createSimple(ModuleEventDispatcher.class);
    private static final DotName OBJECT = DotName.createSimple(Object.class);
    private static final DotName DEFAULT = DotName.createSimple(Default.class);
    private static final MethodDesc MAP_PUT = MethodDesc.of(Map.class, "put", Object.class, Object.class,
            Object.class);
    private static final MethodDesc NOTIFY = MethodDesc.of(ModuleEventObservers.class, "notify", void.class,
            EventContext.class, String.class);

    /**
     * Finds the listener methods once, for the steps below and other extensions.
     */
    @BuildStep
    ListenerDeclarationsBuildItem listenerDeclarations(CombinedIndexBuildItem index,
            Optional<RegistryRoutingBuildItem> routing, ApplicationIndexBuildItem application,
            List<QualifierRegistrarBuildItem> qualifierRegistrars) {
        IndexView view = index.getIndex();
        Set<DotName> extensionQualifiers = new HashSet<>();
        qualifierRegistrars.forEach(it -> extensionQualifiers.addAll(
                it.getQualifierRegistrar().getAdditionalQualifiers().keySet()));
        List<ListenerDeclaration> routed = routing.map(item -> ListenerDeclarations.routedObservers(view,
                item.getTriggerAnnotation(), name -> Objects.nonNull(application.getIndex().getClassByName(name)),
                extensionQualifiers)).orElse(List.of());
        return new ListenerDeclarationsBuildItem(ListenerDeclarations.annotationTypes(view).keySet(),
                ListenerDeclarations.find(view), routed);
    }

    /**
     * With an event publication registry, the dispatcher delivers to {@code AFTER_SUCCESS} observers, so ArC must not
     * notify them as well.
     */
    @BuildStep
    void vetoRoutedObservers(ListenerDeclarationsBuildItem declarations,
            BuildProducer<ObserverTransformerBuildItem> transformers, BuildProducer<UnremovableBeanBuildItem> unremovable) {
        List<ListenerDeclaration> routed = declarations.getRoutedObservers();
        if (routed.isEmpty()) {
            return;
        }
        Set<String> keys = new HashSet<>();
        Set<DotName> beans = new HashSet<>();
        for (ListenerDeclaration declaration : routed) {
            keys.add(RoutedObserverVeto.key(declaration.method()));
            beans.add(declaration.method().declaringClass().name());
        }
        transformers.produce(new ObserverTransformerBuildItem(new RoutedObserverVeto(keys)));
        // Without its observer method the bean is only looked up by the invoker
        unremovable.produce(UnremovableBeanBuildItem.beanTypes(beans));
    }

    /**
     * Fails with a clear message instead of ArC's unsatisfied dependency when virtual threads are selected without the
     * extension. Separate from the build step because tests cannot remove the extension from the class path.
     */
    static void checkExecutor(ListenerExecutorKind executor, boolean virtualThreadsPresent) {
        if (executor == ListenerExecutorKind.VIRTUAL_THREADS && !virtualThreadsPresent) {
            throw new ConfigurationException("quarkus.modulith.listeners.executor=virtual-threads needs the"
                    + " quarkus-virtual-threads extension: add io.quarkus:quarkus-virtual-threads to the application,"
                    + " or use executor=worker");
        }
    }

    @BuildStep
    void listenerBeans(ListenerDeclarationsBuildItem declarations, Capabilities capabilities,
            LaunchModeBuildItem launchMode, ListenerBuildTimeConfig config,
            BuildProducer<AutoAddScopeBuildItem> autoScopes,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<UnremovableBeanBuildItem> unremovable) {
        boolean test = launchMode.getLaunchMode() == LaunchMode.TEST;
        Set<DotName> annotations = declarations.getAnnotationTypes();
        boolean hasListeners = !declarations.isEmpty();
        // Also used by the event publication registry, removed when nothing injects it
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(capabilities.isPresent(Capability.TRANSACTIONS) ? JtaTransactionSupport.class
                        : NoTransactionSupport.class)
                .build());
        if (!hasListeners && !test) {
            return;
        }
        checkExecutor(config.executor(),
                QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.virtual.threads.VirtualThreads"));
        for (DotName annotation : annotations) {
            autoScopes.produce(AutoAddScopeBuildItem.builder()
                    .containsAnnotations(annotation)
                    .defaultScope(BuiltinScope.SINGLETON)
                    .unremovable()
                    .reason("Declares a module listener method")
                    .build());
        }
        // Listener beans are only looked up by the invokers, never injected, so ArC would remove them as unused.
        unremovable.produce(new UnremovableBeanBuildItem(bean -> bean.isClassBean()
                && annotations.stream().anyMatch(a -> bean.getImplClazz().hasAnnotation(a))));
        // ModuleEventObservers looks the dispatcher up through Arc.container(), nothing injects it
        beans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(ModuleEventDispatcher.class).setUnremovable()
                .build());
        // Injected into the dispatcher, which keeps it
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(config.executor() == ListenerExecutorKind.VIRTUAL_THREADS
                        ? VirtualThreadListenerExecutor.class
                        : WorkerListenerExecutor.class)
                .build());
    }

    /**
     * Runs in the bean registration phase, so that the listener table and the invokers can back synthetic beans.
     */
    @BuildStep
    ModuleListenersBuildItem listeners(BeanRegistrationPhaseBuildItem registration, InvokerFactoryBuildItem invokers,
            CombinedIndexBuildItem index, ModuleModelBuildItem model, ExcludedModulesBuildItem excluded,
            ListenerDeclarationsBuildItem declarations, BuildProducer<ValidationErrorBuildItem> errors,
            BuildProducer<BeanConfiguratorBuildItem> beanConfigurators,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        IndexView view = index.getIndex();
        ConditionCompiler compiler = new ConditionCompiler(view);
        GeneratedClassGizmo2Adaptor output = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources, true);
        List<ListenerData> listeners = new ArrayList<>();
        Map<String, Type> observedTypes = new LinkedHashMap<>();
        Map<String, ClassDesc> invokerClasses = new LinkedHashMap<>();
        Map<String, ClassDesc> conditionClasses = new LinkedHashMap<>();
        List<Throwable> problems = new ArrayList<>();
        Map<String, String> ids = new HashMap<>();
        for (ListenerDeclaration declaration : declarations.getAll()) {
            MethodInfo method = declaration.method();
            AnnotationInstance annotation = declaration.attributes();
            ClassInfo declaringClass = method.declaringClass();
            String className = declaringClass.name().toString();
            String location = className + "#" + method.name();
            if (method.parametersCount() != 1) {
                problems.add(new IllegalStateException(
                        "@ApplicationModuleListener method must declare exactly one parameter, the event: " + location));
                continue;
            }
            if (Modifier.isStatic(method.flags()) || Modifier.isPrivate(method.flags())) {
                problems.add(new IllegalStateException(declaration.routedObserver()
                        ? "The AFTER_SUCCESS observer " + location + " gets event publications from the event"
                                + " publication registry, which cannot call a static or private method: make it an"
                                + " instance method that is not private, or set"
                                + " quarkus.modulith.events.registry-trigger-annotation to route only annotated observers"
                        : "@ApplicationModuleListener method must not be static or private: " + location));
                continue;
            }
            Optional<BeanInfo> bean = registration.getContext().beans().classBeans().withBeanClass(declaringClass.name())
                    .firstResult();
            if (bean.isEmpty()) {
                if (excluded.excludes(className)) {
                    continue; // the module is not part of the current module test
                }
                problems.add(new IllegalStateException(
                        "@ApplicationModuleListener method is not declared on a bean: " + location));
                continue;
            }
            // A routed observer runs like a Spring @TransactionalEventListener: right after the commit, without transaction
            String propagation = enumValue(annotation, "propagation",
                    declaration.routedObserver() ? "SUPPORTS" : "REQUIRES_NEW");
            if (propagation.equals("MANDATORY")) {
                problems.add(new IllegalStateException("@ApplicationModuleListener propagation MANDATORY cannot work,"
                        + " listeners run after the publishing transaction completed: " + location));
                continue;
            }
            Type eventType = method.parameterType(0);
            String id = defaultIfBlank(stringValue(annotation, "id"), listenerId(method));
            String duplicate = ids.putIfAbsent(id, location);
            if (Objects.nonNull(duplicate)) {
                problems.add(new IllegalStateException("Duplicate @ApplicationModuleListener id " + id + " on "
                        + duplicate + " and " + location + ": give the listeners distinct ids"));
                continue;
            }
            String condition = stringValue(annotation, "condition").strip();
            Optional<ConditionNode> parsed;
            try {
                parsed = condition.isEmpty() ? Optional.empty()
                        : Optional.of(ConditionParser.parse(condition,
                                Objects.requireNonNullElse(method.parameterName(0), "event")));
                parsed.ifPresent(node -> compiler.validate(node, eventType));
            } catch (IllegalArgumentException e) {
                problems.add(new IllegalStateException("Invalid @ApplicationModuleListener condition \"" + condition
                        + "\" on " + location + ": " + e.getMessage(), e));
                continue;
            }
            boolean conditional = implementsConditional(view, declaringClass);
            if (parsed.isPresent() || conditional) {
                String conditionClass = className + "_ModulithCondition_" + method.name() + "_"
                        + Integer.toHexString(id.hashCode());
                compiler.generate(output, conditionClass, parsed, eventType,
                        conditional ? Optional.of(declaringClass.name()) : Optional.empty());
                conditionClasses.put(id, ClassDesc.of(conditionClass));
            }
            String module = model.model().flatMap(it -> it.moduleOf(className)).map(ModuleDescriptor::name)
                    .orElse("");
            InvokerInfo invoker = invokers.createInvoker(bean.get(), method).withInstanceLookup().build();
            invokerClasses.put(id, invoker.getClassDesc());
            String observedType = eventType.toString();
            observedTypes.putIfAbsent(observedType, eventType);
            listeners.add(new ListenerData(id, className, method.name(), eventType.name().toString(), module,
                    booleanValue(annotation, "async", !declaration.routedObserver()), className(invoker.getClassDesc()),
                    observedType, condition,
                    TxType.valueOf(propagation), booleanValue(annotation, "readOnlyTransaction", false)));
        }
        if (!problems.isEmpty()) {
            errors.produce(new ValidationErrorBuildItem(problems));
        }
        beanConfigurators.produce(new BeanConfiguratorBuildItem(invokersBean(registration, invokerClasses,
                conditionClasses)));
        return new ModuleListenersBuildItem(listeners, observedTypes);
    }

    /**
     * Registers one synchronous and one asynchronous observer per listened type. A listener of {@code Object} gets
     * observers qualified {@code @Default}, which receive every unqualified event but no qualified one, so ArC still
     * skips firing the context lifecycle events, such as {@code @Initialized(RequestScoped.class)}, nobody else
     * observes. Test support records events with its own test-only bean.
     */
    @BuildStep
    void observers(ObserverRegistrationPhaseBuildItem phase, ModuleListenersBuildItem listeners,
            BuildProducer<ObserverConfiguratorBuildItem> observers) {
        for (Map.Entry<String, Type> observed : listeners.getObservedTypes().entrySet()) {
            for (boolean async : List.of(false, true)) {
                ObserverConfigurator configurator = phase.getContext().configure()
                        .id("modulith-" + observed.getKey() + (async ? "-async" : ""))
                        .beanClass(DISPATCHER)
                        .observedType(observed.getValue())
                        .async(async);
                if (observed.getValue().name().equals(OBJECT)) {
                    configurator.addQualifier(DEFAULT);
                }
                configurator.notify(generation -> {
                    BlockCreator b = generation.notifyMethod();
                    b.invokeStatic(NOTIFY, generation.eventContext(), Const.of(observed.getKey()));
                    b.return_();
                });
                observers.produce(new ObserverConfiguratorBuildItem(configurator));
            }
        }
    }

    /**
     * The bean creating every invoker and condition with a constructor call, so no reflection is needed.
     */
    private static BeanConfigurator<ListenerInvokers> invokersBean(BeanRegistrationPhaseBuildItem registration,
            Map<String, ClassDesc> invokerClasses, Map<String, ClassDesc> conditionClasses) {
        BeanConfigurator<ListenerInvokers> configurator = registration.getContext().configure(ListenerInvokers.class);
        // An application class, so that it can see the invoker classes generated next to the listener beans
        configurator.types(ListenerInvokers.class)
                .scope(Singleton.class)
                .forceApplicationClass()
                .creator(generation -> {
                    BlockCreator b = generation.createMethod();
                    LocalVar invokerMap = b.localVar("invokers", b.new_(ConstructorDesc.of(HashMap.class)));
                    invokerClasses.forEach((id, desc) -> b.invokeInterface(MAP_PUT, invokerMap, Const.of(id),
                            b.new_(desc)));
                    LocalVar conditionMap = b.localVar("conditions", b.new_(ConstructorDesc.of(HashMap.class)));
                    conditionClasses.forEach((id, desc) -> b.invokeInterface(MAP_PUT, conditionMap, Const.of(id),
                            b.new_(desc)));
                    b.return_(b.new_(ConstructorDesc.of(ListenerInvokerMaps.class, Map.class, Map.class), invokerMap,
                            conditionMap));
                });
        configurator.done();
        return configurator;
    }

    /**
     * @return Spring's default listener identifier: the qualified method name and the parameter types, for example
     *         {@code com.acme.Listener.on(com.acme.OrderPlaced)}
     */
    static String listenerId(MethodInfo method) {
        List<String> parameters = new ArrayList<>();
        for (Type parameter : method.parameterTypes()) {
            parameters.add(parameter.name().toString());
        }
        return method.declaringClass().name().toString() + "." + method.name() + "(" + String.join(",", parameters)
                + ")";
    }

    private static boolean implementsConditional(IndexView index, ClassInfo clazz) {
        for (ClassInfo current = clazz; Objects.nonNull(current); current = Optional.ofNullable(current.superName())
                .map(index::getClassByName).orElse(null)) {
            if (current.interfaceNames().contains(CONDITIONAL_EVENT_LISTENER)) {
                return true;
            }
        }
        return false;
    }

    private static String stringValue(AnnotationInstance annotation, String name) {
        AnnotationValue value = Objects.isNull(annotation) ? null : annotation.value(name);
        return Objects.isNull(value) ? "" : value.asString();
    }

    private static String enumValue(AnnotationInstance annotation, String name, String defaultValue) {
        AnnotationValue value = Objects.isNull(annotation) ? null : annotation.value(name);
        return Objects.isNull(value) ? defaultValue : value.asEnum();
    }

    private static boolean booleanValue(AnnotationInstance annotation, String name, boolean defaultValue) {
        AnnotationValue value = Objects.isNull(annotation) ? null : annotation.value(name);
        return Objects.isNull(value) ? defaultValue : value.asBoolean();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value.isBlank() ? defaultValue : value;
    }

    static String className(ClassDesc desc) {
        String descriptor = desc.descriptorString();
        return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }
}
