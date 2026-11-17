package io.quarkiverse.modulith.events.jobrunr.deployment;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration;

import io.quarkiverse.modulith.events.ExternalizationMode;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationBuildItem;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationBuildTimeConfig;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationStrategyBuildItem;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationTransportBuildItem;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;
import io.quarkiverse.modulith.events.jobrunr.runtime.EventExternalizerJobRunrTransport;
import io.quarkiverse.modulith.events.jobrunr.runtime.ExternalizationJobRequest;
import io.quarkiverse.modulith.events.jobrunr.runtime.ExternalizationJobRequestHandler;
import io.quarkiverse.modulith.events.jobrunr.runtime.IndexedDatabaseCreator;
import io.quarkiverse.modulith.events.jobrunr.runtime.JobRunrEventExternalizer;
import io.quarkiverse.modulith.events.jobrunr.runtime.JobRunrExternalizationJobs;
import io.quarkiverse.modulith.events.jobrunr.runtime.NoBrokerTransport;
import io.quarkiverse.modulith.events.jobrunr.runtime.TransactionalStorageProviderProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Build steps of {@code quarkus-modulith-events-jobrunr}: in the outbox externalization mode, JobRunr replaces the
 * outbox on the event publication registry. Every externalized event becomes one JobRunr job per
 * {@link JobRunrExternalizationTransport}, saved in the publishing transaction. In the module listener mode this
 * extension registers nothing.
 */
class EventsJobrunrProcessor {

    static final String FEATURE = "modulith-events-jobrunr";
    static final String TRANSPORT_NAME = "jobrunr";
    private static final DotName JOBRUNR_TRANSPORT = DotName.createSimple(JobRunrExternalizationTransport.class);
    private static final DotName BROKER_TRANSPORT = DotName.createSimple(EventExternalizationTransport.class);
    private static final String REASON = "Quarkus Modulith externalization through JobRunr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Keeps JobRunr, which this extension brings, working on the Quarkus line of the build, in every mode.
     */
    @BuildStep
    AdditionalBeanBuildItem jobRunrOnThisLine() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(JobRunrLine.beanClasses()).build();
    }

    /**
     * Removes the beans of JobRunr's extension that cannot work on the Quarkus line of the build.
     */
    @BuildStep
    List<ExcludedTypeBuildItem> excludedJobRunrBeans() {
        return JobRunrLine.excludedTypes();
    }

    @BuildStep
    ExternalizationStrategyBuildItem strategy(ExternalizationBuildTimeConfig config) {
        return outbox(config) ? new ExternalizationStrategyBuildItem("JobRunr") : null;
    }

    /**
     * Activates externalization when the application sends events only through its own JobRunr transports, like
     * Spring Modulith, which needs no broker for JobRunr.
     */
    @BuildStep
    ExternalizationTransportBuildItem applicationTransports(ExternalizationBuildTimeConfig config,
            CombinedIndexBuildItem index) {
        return outbox(config) && ApplicationBeans.declare(index.getIndex(), JOBRUNR_TRANSPORT)
                ? new ExternalizationTransportBuildItem(TRANSPORT_NAME)
                : null;
    }

    @BuildStep
    void beans(ExternalizationBuildItem externalization, List<ExternalizationTransportBuildItem> transports,
            CombinedIndexBuildItem index, Capabilities capabilities, JobRunrBuildTimeConfiguration jobrunr,
            BuildProducer<AdditionalBeanBuildItem> beans, BuildProducer<ReflectiveClassBuildItem> reflection,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<GeneratedResourceBuildItem> generated,
            BuildProducer<NativeImageResourceBuildItem> resources) {
        if (!externalization.isActive() || externalization.getMode() != ExternalizationMode.OUTBOX) {
            return;
        }
        boolean broker = transports.stream().anyMatch(transport -> !TRANSPORT_NAME.equals(transport.getName()))
                || ApplicationBeans.declare(index.getIndex(), BROKER_TRANSPORT);
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .addBeanClasses(JobRunrExternalizationJobs.class, ExternalizationJobRequestHandler.class)
                .addBeanClass(broker ? EventExternalizerJobRunrTransport.class : NoBrokerTransport.class);
        if (sqlOnAgroal(jobrunr.database().type(), capabilities.isPresent(Capability.AGROAL))) {
            builder.addBeanClass(TransactionalStorageProviderProducer.class);
            List<String> migrations = JobRunrMigrations.list(Thread.currentThread().getContextClassLoader());
            generated.produce(new GeneratedResourceBuildItem(IndexedDatabaseCreator.INDEX,
                    String.join("\n", migrations).getBytes(StandardCharsets.UTF_8)));
            resources.produce(new NativeImageResourceBuildItem(IndexedDatabaseCreator.INDEX));
            resources.produce(new NativeImageResourceBuildItem(migrations));
            proxies.produce(new NativeImageProxyDefinitionBuildItem(Connection.class.getName()));
            reflection.produce(ReflectiveClassBuildItem.builder(Connection.class).methods()
                    .reason(REASON + ": jobs saved in the publishing transaction call the connection through a proxy")
                    .build());
        }
        beans.produce(builder.build());
        // looked up programmatically by the observers of the externalization core
        beans.produce(AdditionalBeanBuildItem.unremovableOf(JobRunrEventExternalizer.class));
        reflection.produce(ReflectiveClassBuildItem.builder(ExternalizationJobRequest.class).constructors().fields()
                .reason(REASON + ": JobRunr reads and writes the job requests as JSON").build());
        reflection.produce(ReflectiveClassBuildItem.builder(ExternalizationJobRequestHandler.class).methods()
                .reason(REASON + ": JobRunr finds and calls the run method of the job request handler by reflection")
                .build());
        reflection.produce(ReflectiveClassBuildItem.builder(Thread.Builder.OfVirtual.class).methods()
                .reason(REASON + ": JobRunr's background job server names its virtual worker threads by reflection,"
                        + " and JobRunr's extension registers only the Thread.Builder interface")
                .build());
    }

    @BuildStep
    void validate(ValidationPhaseBuildItem validation, BuildProducer<ValidationErrorBuildItem> errors) {
        Map<DotName, List<BeanInfo>> byClass = validation.getContext().beans().withBeanType(JOBRUNR_TRANSPORT)
                .stream().collect(Collectors.groupingBy(BeanInfo::getBeanClass, HashMap::new, Collectors.toList()));
        byClass.forEach((beanClass, transports) -> {
            if (transports.size() > 1) {
                errors.produce(new ValidationErrorBuildItem(new IllegalStateException(beanClass + " declares "
                        + transports.size() + " JobRunrExternalizationTransport beans, but JobRunr jobs name a"
                        + " transport by its bean class: declare each transport in a class of its own")));
            }
        });
    }

    /**
     * @return whether JobRunr's Quarkus extension stores the jobs through Agroal, the condition of its SQL storage
     *         provider
     */
    static boolean sqlOnAgroal(Optional<String> databaseType, boolean agroal) {
        return agroal && databaseType.map("sql"::equalsIgnoreCase).orElse(true);
    }

    private static boolean outbox(ExternalizationBuildTimeConfig config) {
        return config.enabled() && config.mode() == ExternalizationMode.OUTBOX;
    }
}
