package io.quarkiverse.modulith.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;

import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.ViolationData;
import io.quarkiverse.modulith.core.data.DependencyData;
import io.quarkiverse.modulith.core.impl.ApplicationModulesLoader;
import io.quarkiverse.modulith.core.impl.ArchUnitSupport;
import io.quarkiverse.modulith.core.util.MermaidDiagram;

/**
 * The application modules of an application, like Spring Modulith's {@code ApplicationModules}.
 * <p>
 * In a plain test, build the model from the application's classes:
 *
 * <pre>
 * ApplicationModules.of(Application.class).verify();
 * </pre>
 *
 * In a Quarkus application the model computed at build time is a bean:
 *
 * <pre>
 * &#64;Inject
 * ApplicationModules modules;
 * </pre>
 *
 * With the default configuration the build already fails on violations, so {@link #verify()} on the bean matters when
 * {@code quarkus.modulith.verification.mode} is {@code warn} or {@code disabled}.
 */
public final class ApplicationModules implements Iterable<ApplicationModule> {

    private final ModelData data;
    private final Map<String, JavaType> index;
    private final Map<String, ApplicationModule> modules;
    private final List<JavaPackage> rootPackages;
    private final List<ApplicationModule> rootModules;
    private final List<String> orderedNames;
    private final Map<String, Integer> positions;
    private final Map<String, Optional<ApplicationModule>> parents = new ConcurrentHashMap<>();
    private final Set<VerificationOptions> verifiedOptions = Collections.newSetFromMap(new WeakHashMap<>());
    private Violations violations;
    private boolean defaultsVerified;

    /**
     * @param data the model, as computed by the analysis or recorded at build time
     */
    public ApplicationModules(ModelData data) {
        this.data = data;
        this.index = new LinkedHashMap<>();
        data.types().stream().map(JavaType::new).forEach(it -> index.put(it.getName(), it));
        this.modules = new LinkedHashMap<>();
        data.modules().forEach(it -> modules.put(it.name(), new ApplicationModule(it, index, false)));
        this.rootPackages = data.rootPackages().stream().map(it -> JavaPackage.of(index.values(), it).toSingle()).toList();
        this.rootModules = rootPackages.stream().map(this::rootModuleFor).toList();
        this.orderedNames = sortTopologically();
        this.positions = IntStream.range(0, orderedNames.size()).boxed()
                .collect(Collectors.toUnmodifiableMap(orderedNames::get, Function.identity()));
        this.violations = data.verified() ? new Violations(data.violations().stream().map(ApplicationModules::toViolation)
                .toList()) : null;
    }

    private ApplicationModule rootModuleFor(JavaPackage pkg) {
        List<String> types = pkg.stream().map(JavaType::getName).toList();
        List<String> exposed = pkg.getExposedTypes().stream().map(JavaType::getName).toList();
        return new ApplicationModule(ModuleData.root(pkg.getName(), types, exposed), index, true);
    }

    private static Violation toViolation(ViolationData data) {
        return new Violation(Violation.Kind.valueOf(data.kind()), data.module(), data.source(), data.target(),
                data.message());
    }

    /**
     * Builds the model of the application whose base package is the package of the given type, without starting
     * Quarkus. {@code @Modulith} (or {@code @Modulithic}) on the type is honored, as are the {@code quarkus.modulith.*}
     * properties of {@code application.properties}. Test output directories are left out.
     */
    public static ApplicationModules of(Class<?> type) {
        return ApplicationModulesLoader.load(type, ApplicationModulesLoader.NOTHING_IGNORED, Optional.empty(),
                Optional.of(""));
    }

    /**
     * Same as {@link #of(Class)}, from a package name. A type of the package annotated with {@code @Modulith} or
     * {@code @Modulithic}, if there is exactly one, provides the metadata.
     */
    public static ApplicationModules of(String basePackage) {
        return ApplicationModulesLoader.load(basePackage, ApplicationModulesLoader.NOTHING_IGNORED, Optional.empty(),
                Optional.of(""));
    }

    /**
     * Same as {@link #of(Class)}, reading the class files from the given locations. {@link ClassLocations#TESTS} builds
     * the model of test fixtures that form an application of their own.
     */
    public static ApplicationModules of(Class<?> type, ClassLocations locations) {
        return ApplicationModulesLoader.load(type, ApplicationModulesLoader.NOTHING_IGNORED,
                Optional.of(ApplicationModulesLoader.filter(locations)), Optional.of(locations));
    }

    public static ApplicationModules of(String basePackage, ClassLocations locations) {
        return ApplicationModulesLoader.load(basePackage, ApplicationModulesLoader.NOTHING_IGNORED,
                Optional.of(ApplicationModulesLoader.filter(locations)), Optional.of(locations));
    }

    /**
     * Same as {@link #of(Class)}, leaving out the classes the ArchUnit predicate matches, for example
     * {@link Filters#withoutModule(String)}.
     */
    public static ApplicationModules of(Class<?> type, DescribedPredicate<? super JavaClass> ignored) {
        return ArchUnitSupport.load(type, ignored);
    }

    public static ApplicationModules of(String basePackage, DescribedPredicate<JavaClass> ignored) {
        return ArchUnitSupport.load(basePackage, ignored);
    }

    /**
     * Same as {@link #of(Class)}, importing only the class files the ArchUnit import option accepts. Test output
     * directories are then left out only if the option says so.
     */
    public static ApplicationModules of(Class<?> type, ImportOption option) {
        return ArchUnitSupport.load(type, option);
    }

    public static ApplicationModules of(String basePackage, ImportOption option) {
        return ArchUnitSupport.load(basePackage, option);
    }

    /**
     * @return the declared system name, empty if none
     */
    public String getSystemName() {
        return data.systemName();
    }

    public String getBasePackage() {
        return data.basePackage();
    }

    /**
     * @return what the application declares about its module structure
     */
    public ModulithMetadata getMetadata() {
        return ModulithMetadata.of(data);
    }

    /**
     * @return the name of the type or package the model was built from
     */
    public Object getSource() {
        return data.source();
    }

    /**
     * @return the root packages, the base package first, each without its sub-packages
     */
    public List<JavaPackage> getRootPackages() {
        return rootPackages;
    }

    /**
     * @return the model data, for recording and export
     */
    public ModelData getModelData() {
        return data;
    }

    /**
     * @return the modules in dependency order: a module comes after the modules it depends on
     */
    public Stream<ApplicationModule> stream() {
        return orderedNames.stream().map(modules::get);
    }

    @Override
    public Iterator<ApplicationModule> iterator() {
        return stream().iterator();
    }

    public Optional<ApplicationModule> getModuleByName(String name) {
        return Optional.ofNullable(modules.get(name));
    }

    /**
     * @return the module that contains the type, root modules excluded
     */
    public Optional<ApplicationModule> getModuleForType(String className) {
        return stream().filter(it -> it.contains(className)).findFirst();
    }

    public Optional<ApplicationModule> getModuleForType(Class<?> type) {
        return getModuleForType(type.getName());
    }

    /**
     * @return the module that contains the type with the given simple or fully qualified name, the root module of a
     *         root package included
     */
    public Optional<ApplicationModule> getModuleByType(String className) {
        return allModules().filter(it -> it.contains(className)).findFirst();
    }

    public Optional<ApplicationModule> getModuleByType(Class<?> type) {
        return getModuleByType(type.getName());
    }

    public Optional<ApplicationModule> getModuleByType(JavaType type) {
        return allModules().filter(it -> it.contains(type)).findFirst();
    }

    /**
     * @return the module whose base package is the given package or contains it, or the root module of the given root
     *         package
     */
    public Optional<ApplicationModule> getModuleForPackage(String packageName) {
        return stream().filter(it -> it.containsPackage(packageName))
                .max(Comparator.comparing(it -> PackageName.of(it.getBasePackage())))
                .or(() -> rootModules.stream().filter(it -> it.hasBasePackage(packageName)).findFirst());
    }

    /**
     * @return whether the type belongs to a module
     */
    public boolean contains(Class<?> type) {
        return getModuleForType(type).isPresent();
    }

    public boolean contains(JavaType type) {
        return stream().anyMatch(it -> it.contains(type));
    }

    /**
     * @return whether the type with the given name lives in one of the root packages themselves
     */
    public boolean withinRootPackages(String className) {
        PackageName candidate = PackageName.ofType(className);
        return rootPackages.stream().anyMatch(it -> it.getPackageName().equals(candidate));
    }

    /**
     * @return the modules every module may depend on
     */
    public List<ApplicationModule> getSharedModules() {
        return data.sharedModules().stream().flatMap(it -> getModuleByName(it).stream()).toList();
    }

    /**
     * @return orders modules so that a module comes after the modules it depends on
     */
    public Comparator<ApplicationModule> getComparator() {
        return Comparator.comparingInt(it -> positions.getOrDefault(it.getName(), -1));
    }

    /**
     * @return orders objects by the module of their type (or of the {@link Class} itself): objects of modules other
     *         modules depend on first, objects outside any module last. Spring Modulith's {@code getComparator()}.
     */
    public Comparator<Object> getObjectComparator() {
        return Comparator.comparingInt(this::moduleIndexOf);
    }

    private int moduleIndexOf(Object object) {
        Class<?> type = object instanceof Class<?> it ? it : object.getClass();
        return getModuleForType(type).map(it -> positions.get(it.getName())).orElse(Integer.MAX_VALUE);
    }

    /**
     * @return the module the given nested module belongs to
     */
    public Optional<ApplicationModule> getParentOf(ApplicationModule module) {
        return parents.computeIfAbsent(module.getName(), it -> module.getParentModule(this));
    }

    public boolean hasParent(ApplicationModule module) {
        return getParentOf(module).isPresent();
    }

    /**
     * @return the modules nested in the given module, transitively
     */
    public List<ApplicationModule> getNestedModules(ApplicationModule module) {
        return module.getNestedModules(this);
    }

    /**
     * @return the violations of the module rules and of the default additional verifications
     */
    public Violations detectViolations() {
        return detectViolations(VerificationOptions.defaults());
    }

    /**
     * @return the violations of the module rules and of the additional verifications of the given options
     */
    public Violations detectViolations(VerificationOptions options) {
        return moduleViolations().and(options.evaluate(this));
    }

    private synchronized Violations moduleViolations() {
        if (Objects.isNull(violations)) {
            violations = ModuleVerifier.verify(this);
        }
        return violations;
    }

    /**
     * Throws {@link Violations} if the module rules or the default additional verifications are broken. Once this
     * verification passed, later calls return right away. Unlike Spring Modulith, a failed verification fails again on
     * every call: in Quarkus the instance is a bean that all tests of an application share.
     *
     * @return this instance
     */
    public synchronized ApplicationModules verify() {
        if (!defaultsVerified) {
            detectViolations().throwIfPresent();
            defaultsVerified = true;
        }
        return this;
    }

    /**
     * Throws {@link Violations} if the module rules or the additional verifications of the given options are broken.
     * The options are always evaluated, whatever earlier verifications found, unless the same options instance passed
     * on this model before.
     *
     * @return this instance
     */
    public synchronized ApplicationModules verify(VerificationOptions options) {
        if (!verifiedOptions.contains(options)) {
            detectViolations(options).throwIfPresent();
            verifiedOptions.add(options);
        }
        return this;
    }

    /**
     * @return the module dependency diagram in Mermaid syntax: the one the build recorded, or one rendered from this
     *         model when it was built by {@link #of(Class)} and its variants
     */
    public String toMermaid() {
        return data.mermaid().isEmpty() ? MermaidDiagram.of(this) : data.mermaid();
    }

    Stream<ApplicationModule> allModules() {
        return Stream.concat(stream(), rootModules.stream());
    }

    /**
     * Orders the module identifiers by dependency level, alphabetically within a level. Falls back to the
     * alphabetical order when the modules form a cycle.
     */
    private List<String> sortTopologically() {
        Map<Integer, Set<String>> levels = new TreeMap<>();
        Map<String, Integer> levelOfModule = new HashMap<>();
        Map<String, Optional<ApplicationModule>> moduleOfType = new HashMap<>();
        Set<String> visited = new HashSet<>();
        boolean cycle = new TreeSet<>(modules.keySet()).stream()
                .anyMatch(it -> visit(modules.get(it), visited, new HashSet<>(), levels, levelOfModule, moduleOfType));
        return cycle ? new ArrayList<>(new TreeSet<>(modules.keySet()))
                : levels.values().stream().flatMap(Set::stream).toList();
    }

    private boolean visit(ApplicationModule module, Set<String> visited, Set<String> inProgress,
            Map<Integer, Set<String>> levels, Map<String, Integer> levelOfModule,
            Map<String, Optional<ApplicationModule>> moduleOfType) {
        if (inProgress.contains(module.getName())) {
            return true;
        }
        if (visited.contains(module.getName())) {
            return false;
        }
        inProgress.add(module.getName());
        List<ApplicationModule> dependencies = directDependencyModules(module, moduleOfType);
        if (dependencies.stream()
                .anyMatch(it -> visit(it, visited, inProgress, levels, levelOfModule, moduleOfType))) {
            return true;
        }
        inProgress.remove(module.getName());
        visited.add(module.getName());
        int level = dependencies.stream().mapToInt(it -> levelOfModule.getOrDefault(it.getName(), -1)).max().orElse(-1)
                + 1;
        levels.computeIfAbsent(level, it -> new TreeSet<>()).add(module.getName());
        levelOfModule.put(module.getName(), level);
        return false;
    }

    /**
     * @param moduleOfType the module of each target type looked up so far, as the modules are not ordered yet
     */
    private List<ApplicationModule> directDependencyModules(ApplicationModule module,
            Map<String, Optional<ApplicationModule>> moduleOfType) {
        return module.getQualifiedDependencyData().stream()
                .map(DependencyData::target)
                .distinct()
                .flatMap(target -> moduleOfType.computeIfAbsent(target,
                        it -> modules.values().stream().filter(candidate -> candidate.contains(it)).findFirst()).stream())
                .distinct()
                .toList();
    }

    @Override
    public String toString() {
        return stream().sorted().map(it -> it.toString(this)).collect(Collectors.joining("\n"));
    }
}
