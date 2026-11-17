package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Calls the build steps whose output only native builds consume, so JVM-mode application tests do not run them.
 */
class MomentsProcessorTest {

    final MomentsProcessor processor = new MomentsProcessor();

    final List<AdditionalBeanBuildItem> beans = new ArrayList<>();

    final List<ReflectiveClassBuildItem> reflection = new ArrayList<>();

    @Test
    void registersTheMomentTypesForReflection() {
        // act
        ReflectiveClassBuildItem item = processor.momentTypes();

        // assert
        assertThat(item.getClassNames()).containsExactly(
                "io.quarkiverse.modulith.moments.SecondHasPassed",
                "io.quarkiverse.modulith.moments.MinuteHasPassed",
                "io.quarkiverse.modulith.moments.HourHasPassed",
                "io.quarkiverse.modulith.moments.DayHasPassed",
                "io.quarkiverse.modulith.moments.WeekHasPassed",
                "io.quarkiverse.modulith.moments.MonthHasPassed",
                "io.quarkiverse.modulith.moments.QuarterHasPassed",
                "io.quarkiverse.modulith.moments.YearHasPassed",
                "io.quarkiverse.modulith.moments.ShiftedQuarter",
                "io.quarkiverse.modulith.moments.Quarter");
        assertThat(item.isMethods()).isTrue();
        assertThat(item.isFields()).isTrue();
        assertThat(item.isConstructors()).isTrue();
        assertThat(item.getReason())
                .isEqualTo("Moment events are serialized by the event publication registry and by externalization");
    }

    @Test
    void registersTheJacksonSupportWhenJacksonIsPresent() {
        // arrange
        Capabilities capabilities = new Capabilities(Set.of(Capability.JACKSON));

        // act
        processor.jackson(capabilities, beans::add, reflection::add);

        // assert
        assertThat(beans).hasSize(1);
        assertThat(beans.get(0).getBeanClasses())
                .containsExactly("io.quarkiverse.modulith.moments.runtime.jackson.MomentsJacksonCustomizer");
        assertThat(reflection).singleElement().satisfies(item -> {
            assertThat(item.getClassNames()).containsExactly(
                    "io.quarkiverse.modulith.moments.runtime.jackson.DayHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.HourHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.MinuteHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.MonthHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.QuarterHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.SecondHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.ShiftedQuarterMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.WeekHasPassedMixin",
                    "io.quarkiverse.modulith.moments.runtime.jackson.YearHasPassedMixin");
            assertThat(item.isMethods()).isTrue();
            assertThat(item.isFields()).isFalse();
            assertThat(item.getReason()).isEqualTo("Jackson reads the annotations of the moment mix-ins");
        });
    }

    @Test
    void registersNoJacksonSupportWithoutJackson() {
        // arrange
        Capabilities capabilities = new Capabilities(Set.of());

        // act
        processor.jackson(capabilities, beans::add, reflection::add);

        // assert
        assertThat(beans).isEmpty();
        assertThat(reflection).isEmpty();
    }
}
