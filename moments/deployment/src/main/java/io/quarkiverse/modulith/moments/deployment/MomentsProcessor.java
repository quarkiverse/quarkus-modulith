package io.quarkiverse.modulith.moments.deployment;

import java.util.List;
import java.util.stream.Stream;

import io.quarkiverse.modulith.moments.DayHasPassed;
import io.quarkiverse.modulith.moments.HourHasPassed;
import io.quarkiverse.modulith.moments.MinuteHasPassed;
import io.quarkiverse.modulith.moments.MonthHasPassed;
import io.quarkiverse.modulith.moments.Quarter;
import io.quarkiverse.modulith.moments.QuarterHasPassed;
import io.quarkiverse.modulith.moments.SecondHasPassed;
import io.quarkiverse.modulith.moments.ShiftedQuarter;
import io.quarkiverse.modulith.moments.WeekHasPassed;
import io.quarkiverse.modulith.moments.YearHasPassed;
import io.quarkiverse.modulith.moments.runtime.MomentsProducer;
import io.quarkiverse.modulith.moments.runtime.TimeMachineProducer;
import io.quarkiverse.modulith.moments.runtime.jackson.MomentsJacksonCustomizer;
import io.quarkiverse.modulith.moments.runtime.jackson.MomentsMixins;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.scheduler.deployment.ForceStartSchedulerBuildItem;

/**
 * Build steps of {@code quarkus-modulith-moments}: the {@code Moments} or {@code TimeMachine} bean, the bean that
 * registers the scheduler jobs, Jackson support and native registrations.
 */
class MomentsProcessor {

    static final String FEATURE = "modulith-moments";

    /**
     * Package-private, so registered by name.
     */
    static final String JOBS_BEAN = "io.quarkiverse.modulith.moments.support.MomentsJobs";

    static final List<String> MOMENT_TYPES = Stream.of(SecondHasPassed.class, MinuteHasPassed.class,
            HourHasPassed.class, DayHasPassed.class, WeekHasPassed.class, MonthHasPassed.class, QuarterHasPassed.class,
            YearHasPassed.class, ShiftedQuarter.class, Quarter.class)
            .map(Class::getName)
            .toList();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = MomentsEnabled.class)
    AdditionalBeanBuildItem beans(MomentsBuildTimeConfig config) {
        String producer = config.enableTimeMachine() ? TimeMachineProducer.class.getName()
                : MomentsProducer.class.getName();
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(producer, JOBS_BEAN)
                .build();
    }

    @BuildStep(onlyIf = MomentsEnabled.class)
    ForceStartSchedulerBuildItem startScheduler() {
        return new ForceStartSchedulerBuildItem();
    }

    /**
     * Also without {@code MomentsEnabled}: publications of moments stored while the feature was on must still be read
     * after it is turned off.
     */
    @BuildStep
    ReflectiveClassBuildItem momentTypes() {
        return ReflectiveClassBuildItem.builder(MOMENT_TYPES.toArray(String[]::new))
                .methods()
                .fields()
                .reason("Moment events are serialized by the event publication registry and by externalization")
                .build();
    }

    /**
     * Also without {@code MomentsEnabled}, for the same reason as {@link #momentTypes()}.
     */
    @BuildStep
    void jackson(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ReflectiveClassBuildItem> reflection) {
        if (!capabilities.isPresent(Capability.JACKSON)) {
            return;
        }
        beans.produce(new AdditionalBeanBuildItem(MomentsJacksonCustomizer.class));
        reflection.produce(ReflectiveClassBuildItem
                .builder(MomentsMixins.MIXINS.values().stream().map(Class::getName).sorted().toArray(String[]::new))
                .methods()
                .reason("Jackson reads the annotations of the moment mix-ins")
                .build());
    }
}
