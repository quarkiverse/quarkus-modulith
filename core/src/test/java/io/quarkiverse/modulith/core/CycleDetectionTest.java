package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.data.DependencyData;

/**
 * Cycle detection on models built from data: in a complete graph of {@code n} modules every module depends on every
 * other one, which makes the number of elementary cycles grow exponentially with {@code n}.
 */
class CycleDetectionTest {

    private static final String BASE = "com.acme";

    private static String name(int index) {
        return "m%02d".formatted(index);
    }

    private static ModuleData module(int index, int count) {
        List<DependencyData> dependencies = IntStream.range(0, count).filter(it -> it != index)
                .mapToObj(it -> new DependencyData(BASE + "." + name(index) + ".A", BASE + "." + name(it) + ".A",
                        DependencyType.USES_COMPONENT.name(), name(index) + " uses " + name(it), ""))
                .toList();
        return new ModuleData(name(index), name(index), BASE + "." + name(index), false, false, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), dependencies, List.of(), List.of(), List.of(),
                List.of(), List.of());
    }

    private static ApplicationModules completeGraph(int count) {
        List<ModuleData> modules = IntStream.range(0, count).mapToObj(it -> module(it, count)).toList();
        return new ApplicationModules(new ModelData("", BASE, modules, List.of(), ""));
    }

    @Test
    @Timeout(10)
    void stopsAfterTheDefaultNumberOfCycles() {
        // arrange
        ApplicationModules modules = completeGraph(12);

        // act
        List<Violation> violations = ModuleVerifier.cycles(modules);

        // assert
        assertThat(violations).hasSize(ModuleVerifier.MAX_CYCLES + 1);
        assertThat(violations).extracting(Violation::kind).containsOnly(Violation.Kind.CYCLE);
        assertThat(violations.get(0).source()).isEqualTo("m00 -> m01 -> m00");
        assertThat(violations.get(ModuleVerifier.MAX_CYCLES).message()).isEqualTo(
                "Cycle detection stopped after 100 cycles, further cycles are omitted. Break the cycles above and verify"
                        + " again.");
    }

    @ParameterizedTest
    @CsvSource({ "2,1,1,false", "3,5,5,false", "3,4,5,true", "4,20,20,false", "4,19,20,true" })
    void reportsCyclesUpToTheLimit(int count, int limit, int size, boolean omitted) {
        // arrange
        ApplicationModules modules = completeGraph(count);

        // act
        List<Violation> violations = ModuleVerifier.cycles(modules, limit);

        // assert
        assertThat(violations).hasSize(size);
        assertThat(violations.get(violations.size() - 1).message().startsWith("Cycle detection stopped"))
                .isEqualTo(omitted);
    }

    @Test
    void findsEveryCycleOfASmallGraphInSearchOrder() {
        // arrange
        ApplicationModules modules = completeGraph(3);

        // act
        List<String> cycles = ModuleVerifier.cycles(modules).stream().map(Violation::source).toList();

        // assert
        assertThat(cycles).containsExactly("m00 -> m01 -> m00", "m00 -> m01 -> m02 -> m00", "m00 -> m02 -> m00",
                "m00 -> m02 -> m01 -> m00", "m01 -> m02 -> m01");
    }

    @Test
    void describesEachStepOfACycle() {
        // arrange
        ApplicationModules modules = completeGraph(2);

        // act
        List<String> messages = ModuleVerifier.cycles(modules).stream().map(Violation::message).toList();

        // assert
        assertThat(messages).containsExactly("Cycle detected: m00 -> m01 -> m00\n  1. Dependencies of m00\n"
                + "    - m00 uses m01\n  2. Dependencies of m01\n    - m01 uses m00");
    }

    @Test
    @Timeout(10)
    void skipsPathsThatCannotCloseACycle() {
        // arrange
        int count = 40;
        List<ModuleData> chain = IntStream.range(0, count).mapToObj(index -> {
            List<DependencyData> dependencies = IntStream.range(index + 1, count)
                    .mapToObj(it -> new DependencyData(BASE + "." + name(index) + ".A", BASE + "." + name(it) + ".A",
                            DependencyType.USES_COMPONENT.name(), name(index) + " uses " + name(it), ""))
                    .toList();
            return new ModuleData(name(index), name(index), BASE + "." + name(index), false, false, List.of(),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), dependencies, List.of(), List.of(),
                    List.of(), List.of(), List.of());
        }).toList();
        ApplicationModules modules = new ApplicationModules(new ModelData("", BASE, chain, List.of(), ""));

        // act
        List<Violation> violations = ModuleVerifier.cycles(modules);

        // assert
        assertThat(violations).isEmpty();
    }
}
