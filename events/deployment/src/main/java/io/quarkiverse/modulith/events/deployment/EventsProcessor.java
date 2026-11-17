package io.quarkiverse.modulith.events.deployment;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.deployment.ModuleListenersBuildItem;
import io.quarkiverse.modulith.deployment.events.RegistryRoutingBuildItem;
import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jackson.JacksonEventSerializer;
import io.quarkiverse.modulith.events.registry.DefaultAbandonedEventPublications;
import io.quarkiverse.modulith.events.registry.DefaultEventPublicationRegistry;
import io.quarkiverse.modulith.events.registry.EventPublicationJobs;
import io.quarkiverse.modulith.events.registry.SchedulerPeriodicJobs;
import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkiverse.modulith.runtime.events.ListenerData;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

class EventsProcessor {

    private static final String FEATURE = "modulith-events";
    private static final DotName REPOSITORY = DotName.createSimple(EventPublicationRepository.class);
    private static final String JDK_PACKAGE = "java.";
    private static final Logger LOG = Logger.getLogger(EventsProcessor.class);
    static final List<String> JOB_KEYS = List.of("quarkus.modulith.events.republish-interval",
            "quarkus.modulith.events.delete-completed-interval", "quarkus.modulith.events.staleness.published",
            "quarkus.modulith.events.staleness.processing", "quarkus.modulith.events.staleness.resubmitted");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem serializer() {
        return AdditionalBeanBuildItem.unremovableOf(JacksonEventSerializer.class);
    }

    /**
     * The registry exists when a store extension provides the capability, or when the application declares its own
     * {@link EventPublicationRepository}.
     */
    @BuildStep
    void registry(Capabilities capabilities, CombinedIndexBuildItem index, EventsFixedConfig config,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> configDefaults,
            BuildProducer<RegistryRoutingBuildItem> routing) {
        if (!hasRepository(capabilities, index.getIndex())) {
            return;
        }
        routing.produce(new RegistryRoutingBuildItem(config.registryTriggerAnnotation().map(DotName::createSimple)));
        AdditionalBeanBuildItem.Builder registry = AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultEventPublicationRegistry.class, DefaultAbandonedEventPublications.class,
                        EventPublicationJobs.class)
                .setUnremovable();
        // The jobs are registered programmatically, so the scheduler must start without @Scheduled methods. It only
        // does when the build sees a job configured, so applications without jobs run no scheduler threads.
        if (capabilities.isPresent(Capability.SCHEDULER) && jobConfigured(ConfigProvider.getConfig())) {
            registry.addBeanClass(SchedulerPeriodicJobs.class);
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem("quarkus.scheduler.start-mode", "forced"));
        }
        beans.produce(registry.build());
    }

    /**
     * Stored events are written to and read from JSON, which needs reflection in native executables. The registry
     * stores the concrete class of each event, which can be any subtype of a listened type, and the serializer walks
     * field types, so the whole hierarchy and every known subtype is registered. A listener of a JDK type such as
     * {@code Object} or {@code Serializable} would register almost the whole index, so for those only the subtypes in
     * the application are registered, with a warning.
     */
    @BuildStep
    void registerEventTypesForReflection(ModuleListenersBuildItem listeners, CombinedIndexBuildItem index,
            BuildProducer<ReflectiveHierarchyBuildItem> reflection) {
        IndexView view = index.getIndex();
        for (DotName type : serializedTypes(listeners, view, GeneratedClassGizmoAdaptor::isApplicationClass)) {
            reflection.produce(ReflectiveHierarchyBuildItem.builder(type)
                    .index(view)
                    .source("Module events are serialized for the event publication registry")
                    .build());
        }
    }

    /**
     * @param applicationClass whether a class, by name, belongs to the application
     */
    static Set<DotName> serializedTypes(ModuleListenersBuildItem listeners, IndexView index,
            Predicate<String> applicationClass) {
        Set<DotName> types = new LinkedHashSet<>();
        for (ListenerData listener : listeners.getListeners()) {
            DotName type = DotName.createSimple(listener.eventType());
            boolean jdkType = type.toString().startsWith(JDK_PACKAGE);
            if (jdkType) {
                LOG.warnf("Listener %s takes %s: only its subtypes in the application are registered for reflection"
                        + " in native executables, annotate other event types with @RegisterForReflection", listener.id(),
                        type);
            } else {
                types.add(type);
            }
            Stream.concat(index.getAllKnownSubclasses(type).stream(), index.getAllKnownImplementations(type).stream())
                    .map(ClassInfo::name)
                    .filter(name -> !jdkType || applicationClass.test(name.toString()))
                    .forEach(types::add);
        }
        return types;
    }

    /**
     * @return whether the build-time view of the configuration (application.properties, system properties) sets an
     *         interval or a staleness threshold
     */
    static boolean jobConfigured(Config config) {
        return JOB_KEYS.stream().anyMatch(key -> config.getOptionalValue(key, String.class).isPresent());
    }

    static boolean hasRepository(Capabilities capabilities, IndexView index) {
        return capabilities.isPresent(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY)
                || !index.getAllKnownImplementations(REPOSITORY).isEmpty();
    }
}
