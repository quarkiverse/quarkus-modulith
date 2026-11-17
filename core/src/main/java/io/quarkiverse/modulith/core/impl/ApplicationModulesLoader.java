package io.quarkiverse.modulith.core.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.PackageName;

/**
 * Builds {@link ApplicationModules} in a plain test from the class path, and caches the result per class loader, source
 * and settings like Spring Modulith does.
 */
public final class ApplicationModulesLoader {

    private static final System.Logger LOG = System.getLogger(ApplicationModulesLoader.class.getName());
    private static final Map<ClassLoader, Map<List<Object>, ApplicationModules>> CACHE = new WeakHashMap<>();

    /**
     * Leaves no class out.
     */
    public static final Predicate<String> NOTHING_IGNORED = name -> false;

    private ApplicationModulesLoader() {
    }

    /**
     * @param type the application type, its package is the base package
     * @param ignored classes to leave out
     * @param locations class file locations to read, empty to leave out test output directories unless
     *        {@code quarkus.modulith.verification.include-test-classes} is set
     * @param key identifies the filters in the cache, empty to not cache the model because the filters have no value
     *        equality
     */
    public static ApplicationModules load(Class<?> type, Predicate<String> ignored, Optional<Predicate<URI>> locations,
            Optional<Object> key) {
        return load(type.getPackageName(), Optional.of(type.getName()), ignored, locations, key);
    }

    /**
     * @param basePackage the base package, a type in it annotated with {@code @Modulith} provides the metadata
     */
    public static ApplicationModules load(String basePackage, Predicate<String> ignored,
            Optional<Predicate<URI>> locations, Optional<Object> key) {
        return load(basePackage, Optional.empty(), ignored, locations, key);
    }

    /**
     * Caches the model per class loader, so that a new class loader, for example after a restart of continuous
     * testing, analyses the changed classes again. The analysis runs outside of any lock: it may run code of the
     * application, such as a detection strategy, that builds another model.
     */
    private static ApplicationModules load(String basePackage, Optional<String> modulithType, Predicate<String> ignored,
            Optional<Predicate<URI>> locations, Optional<Object> key) {
        requireAsm(ClassPresence::isPresent);
        ClassLoader classLoader = classLoader();
        ModulithProperties properties = ModulithProperties.load(classLoader);
        Supplier<ApplicationModules> analysis = () -> analyze(basePackage, modulithType, ignored, locations, classLoader,
                properties);
        return key.map(it -> List.of(basePackage, modulithType, it, properties))
                .map(it -> cached(cacheOf(classLoader), it, analysis))
                .orElseGet(analysis);
    }

    private static Map<List<Object>, ApplicationModules> cacheOf(ClassLoader classLoader) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(classLoader, it -> new ConcurrentHashMap<>());
        }
    }

    private static ApplicationModules cached(Map<List<Object>, ApplicationModules> cache, List<Object> key,
            Supplier<ApplicationModules> analysis) {
        return Optional.ofNullable(cache.get(key)).orElseGet(() -> {
            ApplicationModules modules = analysis.get();
            return Objects.requireNonNullElse(cache.putIfAbsent(key, modules), modules);
        });
    }

    private static ApplicationModules analyze(String basePackage, Optional<String> modulithType,
            Predicate<String> ignored, Optional<Predicate<URI>> locations, ClassLoader classLoader,
            ModulithProperties properties) {
        Predicate<URI> accepted = locations.orElseGet(() -> filter(
                properties.flag("verification.include-test-classes", false) ? ClassLocations.ALL
                        : ClassLocations.PRODUCTION));
        ClassPathFiles files = new ClassPathFiles(classLoader, accepted);
        boolean springAnnotations = properties.flag("spring-annotations.enabled", true);
        Optional<String> source = modulithType
                .or(() -> ModelAnalyzer.findModulithType(files, basePackage, springAnnotations));
        String base = source.map(PackageName::packageOf).orElse(basePackage);
        AnalysisResult result = ModelAnalyzer.analyze(properties.settings(base, source, ignored, classLoader), files);
        result.warnings().forEach(warning -> LOG.log(System.Logger.Level.WARNING, warning));
        return new ApplicationModules(result.data());
    }

    /**
     * Fails with an explanation when ASM, which reads the class files and is an optional dependency of the core, is
     * missing from the class path.
     */
    static void requireAsm(Predicate<String> present) {
        if (!present.test("org.objectweb.asm.ClassReader")) {
            throw new IllegalStateException("ApplicationModules.of(...) reads the class files with ASM. Add"
                    + " org.ow2.asm:asm to the test dependencies.");
        }
    }

    /**
     * @return a filter accepting the class file locations of the given kind
     */
    public static Predicate<URI> filter(ClassLocations locations) {
        return switch (locations) {
            case PRODUCTION -> ClassPathFiles::isNotTestOutput;
            case TESTS -> location -> !ClassPathFiles.isNotTestOutput(location);
            case ALL -> location -> true;
        };
    }

    static ClassLoader classLoader() {
        return Objects.requireNonNullElse(Thread.currentThread().getContextClassLoader(),
                ApplicationModulesLoader.class.getClassLoader());
    }
}
