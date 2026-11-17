package io.quarkiverse.modulith.deployment.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkiverse.modulith.ApplicationModule;
import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.Modulith;
import io.quarkiverse.modulith.Modulithic;
import io.quarkiverse.modulith.NamedInterface;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.impl.AnalysisResult;
import io.quarkiverse.modulith.core.impl.AnalysisSettings;
import io.quarkiverse.modulith.core.impl.ClassFiles;
import io.quarkiverse.modulith.core.impl.ModelAnalyzer;

/**
 * Builds the {@link ModuleModel} of an application at build time, with the same analysis
 * {@code ApplicationModules.of} uses in plain tests.
 */
public final class ModuleModelBuilder {

    public static final DotName APPLICATION_MODULE = DotName.createSimple(ApplicationModule.class);
    public static final DotName NAMED_INTERFACE = DotName.createSimple(NamedInterface.class);
    public static final DotName MODULITH = DotName.createSimple(Modulith.class);
    public static final DotName APPLICATION_MODULE_LISTENER = DotName.createSimple(ApplicationModuleListener.class);
    public static final DotName EVENT = DotName.createSimple("jakarta.enterprise.event.Event");
    public static final DotName OBSERVES = DotName.createSimple("jakarta.enterprise.event.Observes");
    public static final DotName OBSERVES_ASYNC = DotName.createSimple("jakarta.enterprise.event.ObservesAsync");

    static final List<DotName> MODULITH_ANNOTATIONS = List.of(MODULITH, DotName.createSimple(Modulithic.class));
    static final List<DotName> SPRING_MODULITH_ANNOTATIONS = List.of(
            DotName.createSimple("org.springframework.modulith.Modulith"),
            DotName.createSimple("org.springframework.modulith.Modulithic"));

    private final IndexView index;
    private final Collection<String> applicationClasses;
    private final ClassFiles files;
    private Optional<String> configuredBasePackage = Optional.empty();
    private UnaryOperator<AnalysisSettings> customizer = UnaryOperator.identity();
    private boolean springAnnotations = true;
    private ClassLoader classLoader = ModuleModelBuilder.class.getClassLoader();
    private Predicate<String> testClasses = it -> false;

    /**
     * @param index index of the application classes, used to find {@code @Modulith}
     * @param applicationClasses names of the classes that belong to the application (not to its libraries)
     * @param classBytes loads the class file of an application class, may return {@code null}
     */
    public ModuleModelBuilder(IndexView index, Collection<String> applicationClasses, Function<String, byte[]> classBytes) {
        this(index, applicationClasses, new MapClassFiles(applicationClasses, classBytes));
    }

    /**
     * @param index index of the application classes, used to find {@code @Modulith}
     * @param applicationClasses names of the classes that belong to the application, used to find {@code @Modulith}
     *        and to infer the base package
     * @param files the class files to analyse
     */
    public ModuleModelBuilder(IndexView index, Collection<String> applicationClasses, ClassFiles files) {
        this.index = index;
        this.applicationClasses = applicationClasses;
        this.files = files;
    }

    public ModuleModelBuilder basePackage(Optional<String> basePackage) {
        this.configuredBasePackage = basePackage;
        return this;
    }

    /**
     * @param springAnnotations whether Spring Modulith's annotations count
     */
    public ModuleModelBuilder springAnnotations(boolean springAnnotations) {
        this.springAnnotations = springAnnotations;
        return this;
    }

    /**
     * @param classLoader reads library class files and loads strategies and source factories
     */
    public ModuleModelBuilder classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * @param testClasses tells the classes of the test sources, which never declare the application's
     *        {@code @Modulith} type, like Spring Modulith reads it from the production classes only
     */
    public ModuleModelBuilder testClasses(Predicate<String> testClasses) {
        this.testClasses = testClasses;
        return this;
    }

    /**
     * @param customizer applies configuration to the analysis settings
     */
    public ModuleModelBuilder settings(UnaryOperator<AnalysisSettings> customizer) {
        this.customizer = customizer;
        return this;
    }

    public ModuleModel build() {
        return new ModuleModel(new ApplicationModules(analyze().data()), true);
    }

    /**
     * @return the model data and the warnings of the analysis
     * @throws IllegalStateException if more than one type is annotated with {@code @Modulith}, or the base package
     *         cannot be determined
     */
    public AnalysisResult analyze() {
        Optional<String> modulithType = findModulith();
        String basePackage = configuredBasePackage
                .orElseGet(() -> modulithType.map(ModuleModelBuilder::packageOf)
                        .orElseGet(() -> commonPackage(applicationClasses).orElseThrow(() -> new IllegalStateException(
                                "Cannot determine the base package of the application. Set"
                                        + " quarkus.modulith.base-package or annotate a type with @Modulith."))));
        AnalysisSettings settings = customizer.apply(new AnalysisSettings(basePackage, modulithType, Optional.empty(),
                List.of(), List.of(), Optional.empty(), "", List.of(), it -> false, springAnnotations, classLoader, true));
        return ModelAnalyzer.analyze(settings, files);
    }

    /**
     * @return the application type annotated with {@code @Modulith} or {@code @Modulithic}, if any
     * @throws IllegalStateException if more than one type is annotated
     */
    public Optional<String> modulithType() {
        return findModulith();
    }

    /**
     * @param testClasses tells the classes of the test sources, whose annotations do not count
     * @return whether a type of the index that is no test class is annotated with {@code @Modulith} or
     *         {@code @Modulithic}
     */
    public static boolean hasModulithType(IndexView index, boolean springAnnotations, Predicate<String> testClasses) {
        return modulithTypes(index, springAnnotations).anyMatch(testClasses.negate());
    }

    private static Stream<String> modulithTypes(IndexView index, boolean springAnnotations) {
        return annotations(springAnnotations)
                .flatMap(it -> index.getAnnotations(it).stream())
                .map(AnnotationInstance::target)
                .map(it -> it.asClass().name().toString());
    }

    private static Stream<DotName> annotations(boolean springAnnotations) {
        return Stream.concat(MODULITH_ANNOTATIONS.stream(),
                springAnnotations ? SPRING_MODULITH_ANNOTATIONS.stream() : Stream.empty());
    }

    private Optional<String> findModulith() {
        List<String> found = modulithTypes(index, springAnnotations)
                .filter(applicationClasses::contains)
                .filter(testClasses.negate())
                .distinct().sorted().toList();
        if (found.size() > 1) {
            throw new IllegalStateException("Only one type may be annotated with @Modulith, found: " + found);
        }
        return found.stream().findFirst();
    }

    /**
     * @return the longest package prefix shared by all the classes, empty when they share none or one of them is in
     *         the default package
     */
    public static Optional<String> commonPackage(Collection<String> classNames) {
        List<String[]> packages = classNames.stream().map(ModuleModelBuilder::packageOf)
                .map(it -> it.isEmpty() ? new String[0] : it.split("\\.")).toList();
        int length = packages.stream().mapToInt(it -> it.length).min().orElse(0);
        int common = 0;
        while (common < length && sameSegment(packages, common)) {
            common++;
        }
        return common == 0 ? Optional.empty()
                : Optional.of(Arrays.stream(packages.get(0), 0, common).collect(Collectors.joining(".")));
    }

    private static boolean sameSegment(List<String[]> packages, int index) {
        return packages.stream().map(it -> it[index]).distinct().count() == 1;
    }

    public static String packageOf(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? "" : className.substring(0, dot);
    }
}
