package io.quarkiverse.modulith.deployment;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.deployment.moduletest.ExcludedModulesBuildItem;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestBuildItem;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestConfig;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestSetup;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTests;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;

/**
 * Module integration tests. Starts only some modules when {@code quarkus.modulith.test.module} is set, as
 * {@code ApplicationModuleTestProfile} does, and records published events in test mode for
 * {@code quarkus-modulith-junit}. The setup is computed and logged once per build.
 */
class ModuleTestProcessor {

    private static final Logger LOG = Logger.getLogger(ModuleTestProcessor.class);

    @BuildStep(onlyIf = IsTest.class)
    ModuleTestBuildItem moduleTest(ModuleModelBuildItem model, ModuleTestConfig config) {
        return new ModuleTestBuildItem(ModuleTests.setup(model.model(), config));
    }

    /**
     * Outside tests the application always starts all modules, so a production build never leaves beans out.
     */
    @BuildStep(onlyIfNot = IsTest.class)
    ModuleTestBuildItem noModuleTest(ModuleTestConfig config) {
        config.module().ifPresent(it -> LOG.warnf("quarkus.modulith.test.module=%s is ignored outside tests, set it"
                + " in the %%test profile", it));
        return new ModuleTestBuildItem(Optional.empty());
    }

    @BuildStep
    ExcludedModulesBuildItem excludedModules(ModuleTestBuildItem moduleTest) {
        return moduleTest.getSetup()
                .map(it -> new ExcludedModulesBuildItem(it.excludedPackages(), it.keptPackages()))
                .orElseGet(() -> new ExcludedModulesBuildItem(Set.of(), Set.of()));
    }

    @BuildStep
    void excludeBeans(ModuleTestBuildItem moduleTest, BuildProducer<ExcludedTypeBuildItem> excludedTypes) {
        moduleTest.getSetup().map(ModuleTestSetup::excludedTypes).orElse(Set.of())
                .forEach(it -> excludedTypes.produce(new ExcludedTypeBuildItem(it)));
    }

    /**
     * Describes the module test for {@code ModuleTestExecution} of {@code quarkus-modulith-junit}.
     */
    @BuildStep
    void describeModuleTest(ModuleTestBuildItem moduleTest, BuildProducer<GeneratedResourceBuildItem> resources) {
        moduleTest.getSetup().ifPresent(it -> resources.produce(new GeneratedResourceBuildItem(
                ModuleTests.SETUP_RESOURCE, it.describe().getBytes(StandardCharsets.ISO_8859_1))));
    }

    /**
     * Registers the event recorder of {@code quarkus-modulith-junit} in test mode, when that module is present. The
     * production event path stays free of test code.
     */
    @BuildStep(onlyIf = IsTest.class)
    void recordPublishedEvents(BuildProducer<AdditionalBeanBuildItem> beans) {
        ModuleTests.recorderBean(QuarkusClassLoader::isClassPresentAtRuntime).ifPresent(beans::produce);
    }

    /**
     * Makes classes annotated with {@code @ApplicationModuleTest} test classes, as {@code @QuarkusTest} classes are.
     * Quarkus only detects test annotations declared in the test sources.
     */
    @BuildStep(onlyIf = IsTest.class)
    void moduleTestAnnotation(BuildProducer<TestAnnotationBuildItem> annotations) {
        ModuleTests.testAnnotation(QuarkusClassLoader::isClassPresentAtRuntime).ifPresent(annotations::produce);
    }
}
