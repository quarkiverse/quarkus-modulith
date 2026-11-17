package io.quarkiverse.modulith.observability.deployment;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.deployment.ModuleModelBuildItem;
import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.observability.runtime.ModuleEntryInterceptor;
import io.quarkiverse.modulith.observability.runtime.ModuleEventObserver;
import io.quarkiverse.modulith.observability.runtime.ModuleObserved;
import io.quarkiverse.modulith.observability.runtime.ObservedModules;
import io.quarkiverse.modulith.observability.runtime.ObservedModulesCreator;
import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Build steps of {@code quarkus-modulith-observability}: Spring Modulith's module observability on OpenTelemetry and
 * Micrometer. Nothing is registered when {@code quarkus.modulith.observability.enabled} is off or neither library is
 * present, so module calls and events then cost nothing extra.
 */
class ObservabilityProcessor {

    static final String FEATURE = "modulith-observability";
    static final String RUNTIME = "io.quarkiverse.modulith.observability.runtime.";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = ObservabilityEnabled.class)
    Optional<ObservedTypesBuildItem> observedTypes(ObservabilityConfig config, Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability, DocumentationModelBuildItem documentation,
            CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> beans) {
        boolean tracing = Observability.tracing(config, capabilities);
        boolean metrics = Observability.metrics(config, metricsCapability);
        if (!tracing && !metrics) {
            return Optional.empty();
        }
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(ModuleObserved.class, ModuleEntryInterceptor.class).build());
        if (tracing) {
            beans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(RUNTIME + "TracingObserver", RUNTIME + "ModuleIdentifierSpanProcessor").build());
        }
        if (metrics) {
            beans.produce(AdditionalBeanBuildItem.builder().addBeanClass(RUNTIME + "TimingObserver").build());
        }
        return Optional.of(new ObservedTypes(documentation.getModel(), index.getIndex()).compute());
    }

    @BuildStep
    void observeTypes(Optional<ObservedTypesBuildItem> types,
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        types.ifPresent(observed -> {
            transformers.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                    .whenClass(it -> observed.getClasses().contains(it.name().toString())
                            || it.interfaceNames().stream().anyMatch(name -> observed.getResources()
                                    .contains(name.toString())))
                    .transform(it -> it.add(ModuleObserved.class))));
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(ObservedModules.class)
                    .scope(Singleton.class)
                    .creator(ObservedModulesCreator.class)
                    .param(ObservedModulesCreator.ROWS, observed.getRows().toArray(String[]::new))
                    .done());
        });
    }

    /**
     * Registers the event counters, and the publication gauges when an event publication store is present. The gauges
     * count in transactions of their own, with the transaction support the core extension registers.
     */
    @BuildStep(onlyIf = ObservabilityEnabled.class)
    void eventBeans(ObservabilityConfig config, Optional<MetricsCapabilityBuildItem> metricsCapability,
            Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<UnremovableBeanBuildItem> unremovable) {
        if (!Observability.metrics(config, metricsCapability)) {
            return;
        }
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(RUNTIME + "ModuleEventMetrics", RUNTIME + "ModuleEventCounterFactory").build());
        // Looked up by the generated event observers through Arc.container(), which removal cannot see
        unremovable.produce(UnremovableBeanBuildItem.beanClassNames(RUNTIME + "ModuleEventMetrics"));
        if (capabilities.isPresent(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY)) {
            beans.produce(AdditionalBeanBuildItem.builder().addBeanClass(RUNTIME + "PublicationGauges").build());
        }
    }

    /**
     * Registers a synchronous and an asynchronous observer per module event type, which hands each event to the
     * counters. The types come from the module model, so no observer of {@code Object} is needed. An observer of a
     * type also receives the events of its subtypes, so a type whose supertype is observed gets no observer of its own,
     * or each of its events would be counted twice.
     */
    @BuildStep(onlyIf = ObservabilityEnabled.class)
    void eventObservers(ObservabilityConfig config, Optional<MetricsCapabilityBuildItem> metricsCapability,
            ModuleModelBuildItem model, CombinedIndexBuildItem index, ObserverRegistrationPhaseBuildItem registration,
            BuildProducer<ObserverConfiguratorBuildItem> observers) {
        if (!Observability.metrics(config, metricsCapability)) {
            return;
        }
        for (String eventType : withoutSubtypes(eventTypes(model), index.getIndex())) {
            for (boolean async : new boolean[] { false, true }) {
                observers.produce(new ObserverConfiguratorBuildItem(registration.getContext().configure()
                        .beanClass(DotName.createSimple(ObservabilityProcessor.class.getName()))
                        .observedType(Type.create(DotName.createSimple(eventType), Type.Kind.CLASS))
                        .async(async)
                        .notify(notify -> {
                            notify.notifyMethod().invokeStatic(MethodDesc.of(
                                    ModuleEventObserver.class, "notify", void.class, EventContext.class),
                                    notify.eventContext());
                            notify.notifyMethod().return_();
                        })));
            }
        }
    }

    /**
     * @return the event types the modules publish or listen to
     */
    static Set<String> eventTypes(ModuleModelBuildItem model) {
        Set<String> result = new TreeSet<>();
        Optional.ofNullable(model.getModel()).ifPresent(it -> {
            for (ModuleDescriptor module : it.modules()) {
                result.addAll(module.publishedEvents());
                result.addAll(module.listenedEvents());
            }
        });
        return result;
    }

    /**
     * @return the types without those that have a supertype among them, as far as the index knows the hierarchy
     */
    static Set<String> withoutSubtypes(Set<String> types, IndexView index) {
        return types.stream()
                .filter(type -> supertypes(type, index).noneMatch(types::contains))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Stream<String> supertypes(String type, IndexView index) {
        ClassInfo info = index.getClassByName(type);
        if (Objects.isNull(info)) {
            return Stream.empty();
        }
        return Stream.concat(Stream.ofNullable(info.superName()), info.interfaceNames().stream())
                .map(DotName::toString)
                .flatMap(it -> Stream.concat(Stream.of(it), supertypes(it, index)));
    }
}
