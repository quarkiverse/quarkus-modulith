package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.StackWalker.StackFrame;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.testkit.engine.EngineTestKit;

import io.quarkiverse.modulith.junit.diff.ModifiedFile;
import io.quarkiverse.modulith.junit.fixtures.app.order.OrderFixture;

class ModulithExecutionConditionTests {

    private static final Supplier<ExecutionSettings> UNUSED_SETTINGS = () -> fail("No settings expected");

    @BeforeEach
    void resetSampleConfigs() {
        SampleConfigs.LOADS.set(0);
    }

    @Test
    void runsEverythingInIde() {
        // arrange
        var condition = condition(true);

        // act
        var result = condition.evaluate(false, Optional.of(OrderFixture.class), UNUSED_SETTINGS);

        // assert
        assertEnabled(result, "Optimization disabled in IDE execution!");
    }

    @Test
    void doesNotFilterTestMethods() {
        // arrange
        var condition = condition(false);

        // act
        var result = condition.evaluate(true, Optional.of(OrderFixture.class), UNUSED_SETTINGS);

        // assert
        assertThat(result.isDisabled()).isFalse();
        assertThat(result.getReason()).get().isIn("Not filtering on a per method basis!",
                "Optimization disabled in IDE execution!");
    }

    @Test
    void doesNotFilterOutsideOfTestClasses() {
        // arrange
        var condition = condition(false);

        // act
        var result = condition.evaluate(false, Optional.empty(), UNUSED_SETTINGS);

        // assert
        assertEnabled(result, "Not a test class context!");
    }

    @Test
    void runsEverythingWithoutChanges() {
        // arrange
        var condition = condition(false);
        var settings = new ExecutionSettings(Changes.NONE, Optional.empty());

        // act
        var result = condition.evaluate(false, Optional.of(OrderFixture.class), () -> settings);

        // assert
        assertEnabled(result, "No changes detected!");
    }

    @Test
    void delegatesToTestExecutionConditionForChanges() {
        // arrange
        var condition = condition(false);
        var settings = new ExecutionSettings(Changes.of(ModifiedFile.of("README.md"), OnNoChange.SKIP_ALL),
                Optional.empty());

        // act
        var result = condition.evaluate(false, Optional.of(OrderFixture.class), () -> settings);

        // assert
        assertThat(result.isDisabled()).isTrue();
        assertThat(result.getReason()).contains(
                "No source file changes detected, tests skipped due to configuration \"on-no-changes=skip-all\".");
    }

    @Test
    void passesConfiguredBasePackageToTestExecutionCondition() {
        // arrange
        var condition = new ModulithExecutionCondition(false, () -> fail("No config expected"), Path.of(""),
                basePackage -> {
                    throw new UnsupportedOperationException(basePackage);
                });
        var settings = new ExecutionSettings(Changes.of(ModifiedFile.of("src/main/java/a/B.java"),
                OnNoChange.EXECUTE_ALL), Optional.of("com.acme"));

        // act
        var result = condition.evaluate(false, Optional.of(OrderFixture.class), () -> settings);

        // assert
        assertEnabled(result, "Application module model of base package com.acme unavailable (com.acme), "
                + "executing test.");
    }

    @Test
    void publicConstructorNeverFiltersTestMethods() {
        // arrange
        var condition = new ModulithExecutionCondition();

        // act
        var result = condition.evaluate(true, Optional.of(OrderFixture.class), UNUSED_SETTINGS);

        // assert
        assertThat(result.isDisabled()).isFalse();
        assertThat(result.getReason()).get().isIn("Not filtering on a per method basis!",
                "Optimization disabled in IDE execution!");
    }

    @ParameterizedTest
    @CsvSource({
            "com.intellij.rt.junit.JUnitStarter, true",
            "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner, true",
            "org.apache.maven.surefire.booter.ForkedBooter, false",
            "org.gradle.process.internal.worker.GradleWorkerMain, false",
            "java.lang.Thread, false"
    })
    void detectsIdeLaunchers(String className, boolean expected) {
        // arrange
        var frames = Stream.<StackFrame> of(new NamedStackFrame("org.junit.Test"), new NamedStackFrame(className));

        // act
        var result = ModulithExecutionCondition.hasIdeRoot(frames);

        // assert
        assertThat(result).isEqualTo(expected);
        assertThat(ModulithExecutionCondition.isIdeClass(className)).isEqualTo(expected);
    }

    @Test
    void usesOutermostFrameForIdeDetection() {
        // arrange
        var frames = Stream.<StackFrame> of(new NamedStackFrame("com.intellij.rt.junit.JUnitStarter"),
                new NamedStackFrame("java.lang.Thread"));

        // act
        var result = ModulithExecutionCondition.hasIdeRoot(frames);

        // assert
        assertThat(result).isFalse();
    }

    @Test
    void detectsNoIdeOnPlainThread() throws Exception {
        // arrange
        var result = new AtomicBoolean(true);
        var thread = Thread.ofPlatform().unstarted(
                () -> result.set(StackWalker.getInstance().walk(ModulithExecutionCondition::hasIdeRoot)));

        // act
        thread.start();
        thread.join();

        // assert
        assertThat(result).isFalse();
    }

    @Test
    void computesSettingsOncePerTestRun() {
        // arrange
        var kit = EngineTestKit.engine("junit-jupiter")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .selectors(selectClass(SkippingSampleA.class), selectClass(SkippingSampleB.class));

        // act
        var results = kit.execute();

        // assert
        results.testEvents().assertStatistics(stats -> stats.started(4).succeeded(4).skipped(0));
        assertThat(SampleConfigs.LOADS).hasValue(1);
    }

    @Test
    void reportsInvalidConfigurationOnTestClass() {
        // arrange
        var kit = EngineTestKit.engine("junit-jupiter")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .selectors(selectClass(InvalidOnNoChangesSample.class));

        // act
        var results = kit.execute();

        // assert
        results.testEvents().assertStatistics(stats -> stats.started(0));
        results.containerEvents().assertThatEvents().haveExactly(1, event(container(InvalidOnNoChangesSample.class),
                finishedWithFailure(cause(instanceOf(IllegalArgumentException.class), message(
                        "Invalid on-no-changes value 'never'! Needs to be any of [execute-all, skip-all, execute-none]!")))));
        assertThat(SampleConfigs.LOADS).hasValue(1);
    }

    private static ModulithExecutionCondition condition(boolean ide) {
        return new ModulithExecutionCondition(ide, () -> fail("No config expected"), Path.of(""),
                ApplicationModulesStub::unavailable);
    }

    private static void assertEnabled(ConditionEvaluationResult result, String message) {
        assertThat(result.isDisabled()).isFalse();
        assertThat(result.getReason()).contains(message);
    }
}
