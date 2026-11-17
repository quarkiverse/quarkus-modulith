package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.deployment.ModuleModelBuildItem;
import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.ExternalizationMode;
import io.quarkiverse.modulith.events.ParsedRoutingTarget;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.externalization.EventExternalizer;
import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.externalization.impl.CompiledRoutingExpressions;
import io.quarkiverse.modulith.events.externalization.impl.DefaultConfigurationCreator;
import io.quarkiverse.modulith.events.externalization.impl.ExternalizationObservers;
import io.quarkiverse.modulith.events.externalization.impl.ExternalizationRedeliveryCreator;
import io.quarkiverse.modulith.events.externalization.impl.RoutingExpression;
import io.quarkiverse.modulith.events.externalization.runtime.EventExternalizerModuleListener;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationPublications;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRedeliveryDecorator;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;
import io.quarkiverse.modulith.events.externalization.runtime.JtaExternalizationTransactions;
import io.quarkiverse.modulith.events.externalization.runtime.NoExternalizationTransactions;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxDrain;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxEventExternalizer;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxResubmissionExclusion;
import io.quarkiverse.modulith.events.externalization.runtime.RoutingResolver;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkiverse.modulith.runtime.events.ListenerBuildTimeConfig;
import io.quarkiverse.modulith.runtime.events.ListenerExecutorKind;
import io.quarkiverse.modulith.runtime.events.VirtualThreadListenerExecutor;
import io.quarkiverse.modulith.runtime.events.WorkerListenerExecutor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Build steps of event externalization: finds the externalized types, compiles their routing expressions, registers
 * the beans of the configured mode and the observers of the event types the application fires.
 */
class ExternalizationProcessor {

    static final String GENERATED_EXPRESSIONS = "io.quarkiverse.modulith.events.externalization.impl."
            + "GeneratedRoutingExpressions";
    private static final Logger LOG = Logger.getLogger(ExternalizationProcessor.class);
    private static final DotName TRANSPORT = DotName.createSimple(EventExternalizationTransport.class);
    private static final DotName PRODUCES = DotName.createSimple(Produces.class);
    private static final DotName CONFIGURATION = DotName.createSimple(EventExternalizationConfiguration.class);
    private static final DotName REDELIVERY = DotName.createSimple(EventRedelivery.class);
    private static final DotName REPOSITORY = DotName.createSimple(EventPublicationRepository.class);
    /**
     * Observers are restricted to events without qualifiers, so that container lifecycle events such as
     * {@code @Initialized(ApplicationScoped.class)} never reach externalization.
     */
    private static final DotName DEFAULT = DotName.createSimple(Default.class);
    private static final String REASON = "Quarkus Modulith event externalization";

    /**
     * A routing expression class is an application class exactly when its event type is, so that it shares the
     * event's class loader in dev and test mode.
     */
    private static final Function<String, String> GENERATED_TO_EVENT_TYPE = RoutingExpressionCompiler::eventTypeOf;

    @BuildStep
    ExternalizationBuildItem externalization(ExternalizationBuildTimeConfig config, CombinedIndexBuildItem index,
            List<ExternalizationTransportBuildItem> transports) {
        boolean transport = !transports.isEmpty() || hasApplicationTransport(index.getIndex());
        LOG.debugf("Externalization transports of extensions: %s",
                transports.stream().map(ExternalizationTransportBuildItem::getName).toList());
        return new ExternalizationBuildItem(config.enabled() && transport, config.mode());
    }

    /**
     * @return whether the application declares a transport bean of its own, as a class or a producer
     */
    static boolean hasApplicationTransport(IndexView index) {
        if (!index.getAllKnownImplementations(TRANSPORT).isEmpty()) {
            return true;
        }
        for (AnnotationInstance produces : index.getAnnotations(PRODUCES)) {
            AnnotationTarget target = produces.target();
            boolean method = target.kind() == AnnotationTarget.Kind.METHOD
                    && target.asMethod().returnType().name().equals(TRANSPORT);
            boolean field = target.kind() == AnnotationTarget.Kind.FIELD
                    && target.asField().type().name().equals(TRANSPORT);
            if (method || field) {
                return true;
            }
        }
        return false;
    }

    @BuildStep
    void beans(ExternalizationBuildItem externalization, Optional<ExternalizationStrategyBuildItem> outbox,
            Capabilities capabilities, ListenerBuildTimeConfig listeners, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (!externalization.isActive()) {
            return;
        }
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(RoutingResolver.class, ExternalizationPublications.class,
                        capabilities.isPresent(Capability.TRANSACTIONS) ? JtaExternalizationTransactions.class
                                : NoExternalizationTransactions.class,
                        // the executor module listeners run on, also when the application has no module listener
                        listeners.executor() == ListenerExecutorKind.VIRTUAL_THREADS
                                ? VirtualThreadListenerExecutor.class
                                : WorkerListenerExecutor.class)
                .build());
        // looked up programmatically by the generated observers and by outbox jobs
        AdditionalBeanBuildItem.Builder unremovable = AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(EventExternalizer.class);
        if (externalization.getMode() == ExternalizationMode.MODULE_LISTENER) {
            unremovable.addBeanClasses(EventExternalizerModuleListener.class, ExternalizationRedeliveryDecorator.class);
        } else if (outbox.isEmpty()) {
            // the exclusion is looked up programmatically by the event publication registry
            unremovable.addBeanClasses(OutboxEventExternalizer.class, OutboxDrain.class,
                    ExternalizationRedeliveryDecorator.class, OutboxResubmissionExclusion.class);
        } else {
            LOG.debugf("The outbox of %s externalizes events", outbox.get().getName());
        }
        beans.produce(unremovable.build());
    }

    @BuildStep
    ExternalizationExpressionsBuildItem externalizedTypes(ExternalizationBuildItem externalization,
            CombinedIndexBuildItem index, ModuleModelBuildItem model,
            BuildProducer<ExternalizedTypeBuildItem> types,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovable,
            BuildProducer<ReflectiveHierarchyBuildItem> hierarchy,
            BuildProducer<ReflectiveClassBuildItem> reflection) {
        if (!externalization.isActive()) {
            return new ExternalizationExpressionsBuildItem(Set.of(), Set.of());
        }
        ExternalizedTypeScanner scanner = new ExternalizedTypeScanner(index.getIndex(), index.getComputingIndex());
        Map<DotName, List<AnnotationInstance>> externalized = scanner.scan();
        Optional<String> basePackage = Optional.ofNullable(model.getModel()).map(found -> found.basePackage());
        RoutingExpressionCompiler compiler = new RoutingExpressionCompiler(index.getComputingIndex(),
                NamedBeans.scan(index.getIndex()),
                new GeneratedClassGizmoAdaptor(generatedClasses, GENERATED_TO_EVENT_TYPE),
                GeneratedClassGizmoAdaptor::isApplicationClass);
        List<String> problems = new ArrayList<>();
        List<CompiledExpression> compiled = new ArrayList<>();
        Set<String> beanNames = new TreeSet<>();
        externalized.forEach((type, annotations) -> {
            String declaration = ExternalizedTypeScanner.declaration(annotations);
            types.produce(new ExternalizedTypeBuildItem(type.toString(), declaration,
                    staticTarget(type.toString(), declaration, basePackage)));
            hierarchy.produce(ReflectiveHierarchyBuildItem.builder(type).source(REASON).build());
            for (String expression : expressions(ExternalizedTypeScanner.declarations(annotations))) {
                try {
                    ExpressionTemplate template = RoutingExpressionParser.parse(expression);
                    beanNames.addAll(beanNames(template));
                    String generated = compiler.compile(index.getComputingIndex().getClassByName(type), template);
                    compiled.add(new CompiledExpression(type.toString(), expression, generated));
                } catch (RoutingExpressionException e) {
                    problems.add("@Externalized on " + type + ": " + e.getMessage());
                }
            }
        });
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Invalid routing expressions:\n" + String.join("\n", problems));
        }
        generateExpressionTable(compiled, generatedBeans);
        Set<String> calledBeans = new HashSet<>(beanNames);
        unremovable.produce(new UnremovableBeanBuildItem(bean -> calledBeans.contains(bean.getName())));
        reflection.produce(ReflectiveClassBuildItem.builder(scanner.annotationTypes().stream()
                .map(DotName::toString).toArray(String[]::new)).methods().reason(REASON).build());
        return new ExternalizationExpressionsBuildItem(beanNames, Set.copyOf(externalized.keySet().stream()
                .map(DotName::toString).toList()));
    }

    /**
     * @return the target the default configuration routes the type to, empty when the declared target is an
     *         expression
     */
    static Optional<String> staticTarget(String typeName, String declaration, Optional<String> basePackage) {
        Optional<String> declared = ParsedRoutingTarget.parse(declaration).getTarget();
        if (declared.isPresent()) {
            return declared.filter(target -> !RoutingTarget.isExpression(target));
        }
        int lastDot = typeName.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : typeName.substring(0, lastDot);
        return Optional.of(basePackage
                .filter(candidate -> packageName.equals(candidate) || packageName.startsWith(candidate + "."))
                .map(candidate -> typeName.substring(candidate.length() + 1))
                .orElse(typeName));
    }

    /**
     * @return the target and key parts of the declarations that are expressions
     */
    static Set<String> expressions(Set<String> declarations) {
        Set<String> result = new TreeSet<>();
        for (String declaration : declarations) {
            ParsedRoutingTarget parsed = ParsedRoutingTarget.parse(declaration);
            parsed.getTarget().filter(RoutingTarget::isExpression).ifPresent(result::add);
            parsed.getKey().filter(RoutingTarget::isExpression).ifPresent(result::add);
        }
        return result;
    }

    private static Set<String> beanNames(ExpressionTemplate template) {
        Set<String> names = new TreeSet<>();
        for (TemplateSegment segment : template.segments()) {
            if (segment instanceof ChainSegment chain) {
                collectBeanNames(chain.chain(), names);
            }
        }
        return names;
    }

    private static void collectBeanNames(ExpressionChain chain, Set<String> names) {
        if (!chain.startsAtEvent()) {
            names.add(chain.beanName());
        }
        for (ExpressionMember member : chain.members()) {
            member.arguments().forEach(argument -> collectBeanNames(argument, names));
        }
    }

    private static void generateExpressionTable(List<CompiledExpression> compiled,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        try (ClassCreator table = ClassCreator.builder().classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                .className(GENERATED_EXPRESSIONS).superClass(CompiledRoutingExpressions.class).build()) {
            table.addAnnotation(Singleton.class);
            MethodCreator constructor = table.getMethodCreator(MethodDescriptor.ofConstructor(GENERATED_EXPRESSIONS));
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(CompiledRoutingExpressions.class),
                    constructor.getThis());
            MethodDescriptor register = MethodDescriptor.ofMethod(CompiledRoutingExpressions.class, "register",
                    void.class, String.class, String.class, RoutingExpression.class);
            for (CompiledExpression entry : compiled) {
                constructor.invokeVirtualMethod(register, constructor.getThis(), constructor.load(entry.type()),
                        constructor.load(entry.expression()),
                        constructor.newInstance(MethodDescriptor.ofConstructor(entry.className())));
            }
            constructor.returnVoid();
        }
    }

    @BuildStep
    void syntheticBeans(ExternalizationBuildItem externalization, Optional<ExternalizationStrategyBuildItem> outbox,
            BeanRegistrationPhaseBuildItem registration, ModuleModelBuildItem model,
            BuildProducer<BeanConfiguratorBuildItem> configurators) {
        if (!externalization.isActive()) {
            return;
        }
        if (registration.getContext().beans().withBeanType(CONFIGURATION).isEmpty()) {
            String[] packages = Optional.ofNullable(model.getModel()).map(found -> new String[] { found.basePackage() })
                    .orElse(new String[0]);
            configurators.produce(new BeanConfiguratorBuildItem(registration.getContext()
                    .configure(EventExternalizationConfiguration.class)
                    .types(EventExternalizationConfiguration.class)
                    .scope(Singleton.class)
                    .param(DefaultConfigurationCreator.PACKAGES, packages)
                    .addInjectionPoint(ClassType.create(ExternalizationRuntimeConfig.class))
                    .creator(DefaultConfigurationCreator.class)));
        }
        Optional.of(registration.getContext())
                .filter(context -> storesPublications(externalization.getMode(), outbox))
                .filter(context -> context.beans().withBeanType(REDELIVERY).isEmpty())
                .ifPresent(context -> configurators.produce(new BeanConfiguratorBuildItem(registration.getContext()
                        .configure(EventRedelivery.class)
                        .types(EventRedelivery.class)
                        .scope(Singleton.class)
                        .addInjectionPoint(ClassType.create(ExternalizationStrategy.class))
                        .creator(ExternalizationRedeliveryCreator.class))));
    }

    /**
     * @return whether the strategy of the mode stores event publications, which the registry resubmits through
     *         {@link EventRedelivery}
     */
    static boolean storesPublications(ExternalizationMode mode, Optional<ExternalizationStrategyBuildItem> outbox) {
        return mode == ExternalizationMode.MODULE_LISTENER || outbox.isEmpty();
    }

    /**
     * @return whether the mode needs an {@link EventPublicationRepository}: the outbox on the registry
     */
    static boolean needsRepository(ExternalizationMode mode, Optional<ExternalizationStrategyBuildItem> outbox) {
        return mode == ExternalizationMode.OUTBOX && outbox.isEmpty();
    }

    @BuildStep
    ObservedEventTypesBuildItem observedTypes(ExternalizationBuildItem externalization,
            BeanRegistrationPhaseBuildItem registration, ExternalizationExpressionsBuildItem expressions,
            CombinedIndexBuildItem index) {
        if (!externalization.isActive()) {
            return new ObservedEventTypesBuildItem(Set.of());
        }
        return new ObservedEventTypesBuildItem(new ObservedEventTypes(index.getComputingIndex(),
                GeneratedClassGizmoAdaptor::isApplicationClass)
                .collect(registration.getInjectionPoints().stream()
                        .map(point -> new EventInjectionPoint(point.getRequiredType(),
                                point.getTargetBean().map(bean -> bean.getBeanClass().toString()).orElse("")))
                        .toList(), expressions.getExternalizedTypes()));
    }

    @BuildStep
    void observers(ObservedEventTypesBuildItem types, ObserverRegistrationPhaseBuildItem registration,
            BuildProducer<ObserverConfiguratorBuildItem> observers) {
        for (DotName type : types.getTypes()) {
            observers.produce(new ObserverConfiguratorBuildItem(registration.getContext().configure()
                    .beanClass(DotName.createSimple(ExternalizationObservers.class))
                    .observedType(ClassType.create(type))
                    .addQualifier(DEFAULT)
                    .notify(generation -> {
                        BlockCreator block = generation.notifyMethod();
                        Expr event = block.invokeInterface(MethodDesc.of(EventContext.class, "getEvent", Object.class),
                                generation.eventContext());
                        block.invokeStatic(MethodDesc.of(ExternalizationObservers.class, "notify", void.class,
                                Object.class), event);
                        block.return_();
                    })));
        }
    }

    @BuildStep
    void validate(ExternalizationBuildItem externalization, Optional<ExternalizationStrategyBuildItem> outbox,
            ExternalizationExpressionsBuildItem expressions, ValidationPhaseBuildItem validation,
            BuildProducer<ValidationErrorBuildItem> errors) {
        if (!externalization.isActive()) {
            return;
        }
        List<Throwable> problems = new ArrayList<>();
        Optional.of(validation.getContext())
                .filter(context -> needsRepository(externalization.getMode(), outbox))
                .filter(context -> context.beans().withBeanType(REPOSITORY).isEmpty())
                .ifPresent(context -> problems
                        .add(new IllegalStateException("The outbox externalization mode records events as event"
                                + " publications and needs an EventPublicationRepository bean: add an event publication store such"
                                + " as quarkus-modulith-events-jpa, or quarkus-modulith-events-jobrunr to use JobRunr as the"
                                + " outbox, or set " + ExternalizationMode.PROPERTY + "=module-listener")));
        for (String name : expressions.getBeanNames()) {
            if (validation.getContext().beans().withName(name).isEmpty()) {
                problems.add(new IllegalStateException("The routing expressions of @Externalized call the bean "
                        + name + ", which is not a CDI bean: give the class annotated with @Named(\"" + name
                        + "\") a scope such as @ApplicationScoped"));
            }
        }
        if (!problems.isEmpty()) {
            errors.produce(new ValidationErrorBuildItem(problems));
        }
    }
}
