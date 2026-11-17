package io.quarkiverse.modulith.deployment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ApplicationModulesFactory;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModulithRule;
import io.quarkiverse.modulith.core.VerificationOptions;
import io.quarkiverse.modulith.core.Violations;
import io.quarkiverse.modulith.core.data.ModelCodec;
import io.quarkiverse.modulith.core.impl.AnalysisResult;
import io.quarkiverse.modulith.core.impl.AnalysisSettings;
import io.quarkiverse.modulith.core.util.ApplicationModulesExporter;
import io.quarkiverse.modulith.deployment.docs.Diagrams;
import io.quarkiverse.modulith.deployment.model.ArchiveClassFiles;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.model.ModuleModelBuilder;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestConfig;
import io.quarkiverse.modulith.runtime.ModulithBuildTimeConfig;
import io.quarkiverse.modulith.runtime.ModulithBuildTimeConfig.Verification.Mode;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.LaunchMode;

/**
 * Builds and verifies the application module model with the analysis of {@code quarkus-modulith-core}. The model is
 * written to a resource for runtime use, and {@code ModelRecordingSteps}, which differs between Quarkus 3 and Quarkus 4,
 * records the small runtime beans.
 */
class ModelProcessor {

    private static final Logger LOG = Logger.getLogger(ModelProcessor.class);
    private static final String FEATURE = "modulith";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ModuleModelBuildItem moduleModel(ApplicationArchivesBuildItem archives, ModulithBuildTimeConfig config,
            ModuleTestConfig moduleTest, LaunchModeBuildItem launchMode) {
        List<ApplicationArchive> applicationArchives = new ArrayList<>();
        applicationArchives.add(archives.getRootArchive());
        applicationArchives.addAll(archives.getArchives());
        IndexView index = CompositeIndex.create(applicationArchives.stream().map(ApplicationArchive::getIndex).toList());
        ArchiveClassFiles files = new ArchiveClassFiles(applicationArchives, config.verification().includeTestClasses());
        Optional<String> basePackage = config.basePackage();
        boolean annotated = ModuleModelBuilder.hasModulithType(index, config.springAnnotations().enabled(),
                files::isTestClass);
        if (basePackage.isEmpty() && !annotated) {
            Optional<String> inferred = ModuleModelBuilder.commonPackage(archives.getRootArchive().getIndex()
                    .getKnownClasses().stream().map(ClassInfo::name).map(Object::toString).collect(Collectors.toSet()));
            if (inferred.isEmpty()) {
                LOG.warn("Quarkus Modulith cannot infer the base package because the application classes share no"
                        + " common package. Set quarkus.modulith.base-package or annotate a type with @Modulith.");
                return new ModuleModelBuildItem(null, "");
            }
            basePackage = inferred;
            LOG.debugf("Using inferred base package %s", inferred.get());
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ModuleModelBuilder builder = new ModuleModelBuilder(index, files.classNames(), files)
                .basePackage(basePackage)
                .springAnnotations(config.springAnnotations().enabled())
                .testClasses(files::isTestClass)
                .classLoader(classLoader)
                .settings(settings -> configure(settings, config));
        if (launchMode.getLaunchMode() == LaunchMode.TEST && moduleTest.module().isPresent()) {
            Optional<ModelData> registered = registeredFactoryModel(builder.modulithType(), classLoader);
            if (registered.isPresent()) {
                LOG.debug("Using the ApplicationModulesFactory registered on the class path for the module test");
                return verify(registered.get(), config);
            }
        }
        AnalysisResult result = builder.analyze();
        result.warnings().forEach(LOG::warn);
        return verify(result.data(), config);
    }

    /**
     * A module test builds the model with the {@link ApplicationModulesFactory} the class path registers, like Spring
     * Modulith's {@code ModuleTestExecution}, when the application has a {@code @Modulith} type to give it.
     *
     * @return the model data of the registered factory, empty without factory or {@code @Modulith} type
     */
    static Optional<ModelData> registeredFactoryModel(Optional<String> modulithType, ClassLoader classLoader) {
        return ApplicationModulesFactory.registered(classLoader)
                .flatMap(factory -> modulithType.map(type -> factory.of(load(type, classLoader)).getModelData()));
    }

    /**
     * Loads the {@code @Modulith} type for an {@link ApplicationModulesFactory}, without initializing it.
     *
     * @throws IllegalStateException if the type cannot be loaded
     */
    static Class<?> load(String type, ClassLoader classLoader) {
        try {
            return Class.forName(type, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load the @Modulith type " + type
                    + " for the registered ApplicationModulesFactory", e);
        }
    }

    static AnalysisSettings configure(AnalysisSettings settings, ModulithBuildTimeConfig config) {
        return new AnalysisSettings(settings.basePackage(), settings.modulithType(), config.systemName(),
                config.sharedModules().orElse(List.of()), config.additionalPackages().orElse(List.of()),
                config.useFullyQualifiedModuleNames(), config.detectionStrategy().orElse(""),
                config.ignoredPackages().orElse(List.of()), settings.ignoredClasses(),
                config.springAnnotations().enabled(), settings.classLoader(),
                config.verification().allowFieldInjection());
    }

    /**
     * Runs the verification as configured and records its result in the model.
     *
     * @throws Violations if the rules are broken and the mode is {@code fail}
     */
    static ModuleModelBuildItem verify(ModelData data, ModulithBuildTimeConfig config) {
        ApplicationModules unverified = new ApplicationModules(data);
        String mermaid = Diagrams.mermaid(new ModuleModel(unverified, false));
        Mode mode = config.verification().mode();
        if (mode == Mode.DISABLED) {
            ModelData recorded = data.withMermaid(mermaid);
            return new ModuleModelBuildItem(new ModuleModel(new ApplicationModules(recorded), false), mermaid);
        }
        Violations violations = unverified.detectViolations(options(config, data));
        ModelData recorded = data.withViolations(violations.getViolations().stream()
                .map(it -> new ModelData.ViolationData(it.kind().name(), it.module(), it.source(), it.target(),
                        it.message()))
                .toList(), mermaid);
        if (violations.hasViolations()) {
            if (mode == Mode.FAIL) {
                throw violations;
            }
            LOG.warn(violations.getMessage());
        }
        ApplicationModules modules = new ApplicationModules(recorded);
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Detected %d application modules in %s: %s", modules.stream().count(), modules.getBasePackage(),
                    modules.stream().map(it -> it.getName() + " listening to " + it.getListenedEvents()).toList());
        }
        return new ModuleModelBuildItem(new ModuleModel(modules, true), mermaid);
    }

    /**
     * @return the additional rules configured: module rules by class name, and the jMolecules rules when enabled
     */
    static VerificationOptions options(ModulithBuildTimeConfig config, ModelData data) {
        VerificationOptions options = VerificationOptions.defaults().withoutAdditionalVerifications()
                .andRules(config.verification().additionalRules().orElse(List.of()).stream()
                        .map(ModelProcessor::rule)
                        .toArray(ModulithRule[]::new));
        return config.verification().jmoleculesRules() ? options.andJMoleculesRules() : options;
    }

    /**
     * Loads an additional rule with the context class loader of the build, an accepted exception to reading
     * application code through Jandex only: a rule is code the build runs. Its static initializer runs in the build
     * class loader, so a rule must not keep state there between builds or touch application beans.
     */
    static ModulithRule rule(String name) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name)
                    .asSubclass(ModulithRule.class).getConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("quarkus.modulith.verification.additional-rules lists " + name
                    + ", which is not a ModulithRule with a public no-argument constructor", e);
        }
    }

    /**
     * Writes the model to the resource the {@code ApplicationModules} bean is read from on first use, and, when
     * {@code quarkus.modulith.export.enabled} is set, in Spring Modulith's JSON format like Spring Modulith's AOT
     * processing.
     */
    @BuildStep
    void exportModel(ModuleModelBuildItem model, ModulithBuildTimeConfig config,
            BuildProducer<GeneratedResourceBuildItem> resources, BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        resources.produce(new GeneratedResourceBuildItem(ModelCodec.LOCATION, ModelCodec.encode(model.getModelData())));
        nativeResources.produce(new NativeImageResourceBuildItem(ModelCodec.LOCATION));
        if (config.export().enabled()) {
            model.getApplicationModules().ifPresent(it -> resources.produce(new GeneratedResourceBuildItem(
                    ApplicationModulesExporter.DEFAULT_LOCATION,
                    new ApplicationModulesExporter(it).toFullJson().getBytes(StandardCharsets.UTF_8))));
        }
    }

    /**
     * {@code ApplicationRuntime#getMainApplicationClass()} loads the {@code @Modulith} type by name.
     */
    @BuildStep
    void registerMainClass(ModuleModelBuildItem model, BuildProducer<ReflectiveClassBuildItem> reflection) {
        mainClass(model).ifPresent(it -> reflection.produce(ReflectiveClassBuildItem.builder(it)
                .reason("ApplicationRuntime#getMainApplicationClass loads the @Modulith type").build()));
    }

    /**
     * @return the name of the {@code @Modulith} type, if the model was built from one
     */
    static Optional<String> mainClass(ModuleModelBuildItem model) {
        return model.model().flatMap(it -> Optional.of(it.applicationModules().getModelData().source())
                .filter(source -> !source.equals(it.basePackage())));
    }
}
