package io.quarkiverse.modulith.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Scope;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.ApplicationModule;
import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.Modulith;
import io.quarkiverse.modulith.Modulithic;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.test.component.QuarkusComponentTestExtension;
import io.quarkus.test.component.QuarkusComponentTestExtensionBuilder;

/**
 * A module slice without starting the application: a {@code QuarkusComponentTest} whose components are the beans of
 * one application module. Dependencies on beans of other modules are mocked by {@code QuarkusComponentTest}. The
 * Quarkus counterpart of combining Spring Modulith's module slicing with a Spring Boot slice test.
 *
 * <pre>
 * class ShippingComponentTest {
 *
 *     &#64;RegisterExtension
 *     static final QuarkusComponentTestExtension extension = ApplicationModuleComponents
 *             .of(ShippingComponentTest.class).build();
 *
 *     &#64;Inject
 *     Shipping shipping;
 *
 *     &#64;InjectMock
 *     Inventory inventory;
 * }
 * </pre>
 *
 * The module is the one that contains the test class: the closest enclosing package whose {@code package-info}
 * declares {@code @ApplicationModule}, else the direct sub-package of the package with the {@code @Modulith} type,
 * else the test class's package. {@link #of(Class, String)} selects a module by identifier instead.
 * <p>
 * The components follow ArC's rules for bean classes: classes that declare a scope or a stereotype (like ArC, a scope
 * inherited from a superclass does not count), classes that declare producer or observer members (dependent beans),
 * and classes that declare {@code @ApplicationModuleListener} methods, which the extension makes singletons. Abstract
 * classes, interfaces, annotations, {@code @Vetoed} classes and packages, and the classes of nested modules
 * (sub-packages that declare {@code @ApplicationModule} themselves) are left out.
 * <p>
 * Not detected, so mocked like the beans of other modules unless added with
 * {@link QuarkusComponentTestExtensionBuilder#addComponentClasses(Class...)}: interceptors and decorators, and classes
 * that other Quarkus extensions turn into beans without a bean defining annotation (for example a {@code @Path}
 * resource, or a class with {@code @Scheduled} or {@code @Incoming} methods).
 * <p>
 * Needs {@code io.quarkus:quarkus-junit-component} on the test class path.
 */
public final class ApplicationModuleComponents {

    private static final DotName PRODUCES = DotName.createSimple(Produces.class);
    private static final DotName LISTENER = DotName.createSimple(ApplicationModuleListener.class);
    private static final DotName OBSERVES = DotName.createSimple(Observes.class);
    private static final DotName OBSERVES_ASYNC = DotName.createSimple(ObservesAsync.class);
    private static final DotName VETOED = DotName.createSimple(Vetoed.class);
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";
    private static final String CLASS_SUFFIX = ".class";

    private static final Logger LOG = Logger.getLogger(ApplicationModuleComponents.class);

    private ApplicationModuleComponents() {
    }

    /**
     * @return a component test builder with the beans of the module that contains the test class
     */
    public static QuarkusComponentTestExtensionBuilder of(Class<?> testClass) {
        return builder(componentClasses(testClass), testClass.getClassLoader());
    }

    /**
     * @param moduleId the identifier of the module, as declared with {@code @ApplicationModule(id = …)} or the name of
     *        its package below the application's base package
     * @return a component test builder with the beans of that module
     * @throws IllegalStateException if no application encloses the test class or it has no module with that
     *         identifier
     */
    public static QuarkusComponentTestExtensionBuilder of(Class<?> testClass, String moduleId) {
        return builder(componentClasses(testClass, moduleId), testClass.getClassLoader());
    }

    /**
     * @return the bean classes of the module that contains the test class, sorted by name
     */
    static List<Class<?>> componentClasses(Class<?> testClass) {
        ClassLoader loader = testClass.getClassLoader();
        return beanClasses(modulePackage(testClass.getPackageName(), loader), loader);
    }

    /**
     * @return the bean classes of the module with the given identifier, sorted by name
     */
    static List<Class<?>> componentClasses(Class<?> testClass, String moduleId) {
        ClassLoader loader = testClass.getClassLoader();
        String basePackage = basePackage(testClass.getPackageName(), loader).orElseThrow(
                () -> new IllegalStateException("No type annotated with @Modulith or @Modulithic encloses "
                        + testClass.getName() + ", so module " + moduleId + " cannot be found"));
        return beanClasses(modulePackageById(basePackage, moduleId, loader), loader);
    }

    private static QuarkusComponentTestExtensionBuilder builder(List<Class<?>> components, ClassLoader loader) {
        return QuarkusComponentTestExtension.builder()
                .addComponentClasses(components.toArray(Class<?>[]::new))
                .addAnnotationsTransformer(impliedScopes(loader));
    }

    /**
     * @return a transformer that gives listener classes {@code @Singleton} and producer or observer classes
     *         {@code @Dependent}, as the application does, because a component test runs neither the extension nor
     *         Quarkus' discovery rules. Each class is classified once.
     */
    @SuppressWarnings("deprecation")
    static AnnotationsTransformer impliedScopes(ClassLoader loader) {
        Map<DotName, ComponentKind> kinds = new ConcurrentHashMap<>();
        Function<ClassInfo, Optional<Class<? extends Annotation>>> impliedScope = type -> kinds
                .computeIfAbsent(type.name(), name -> classify(type, loader)).impliedScope();
        return AnnotationsTransformer.appliedToClass()
                .whenClass(type -> impliedScope.apply(type).isPresent())
                .transform(context -> context.transform()
                        .add(impliedScope.apply(context.getTarget().asClass()).orElseThrow()).done());
    }

    /**
     * @return the package of the module that contains the given package
     */
    static String modulePackage(String packageName, ClassLoader loader) {
        String child = packageName;
        for (String candidate = packageName; !candidate.isEmpty(); candidate = parent(candidate)) {
            if (declaresModule(candidate, loader)) {
                return candidate;
            }
            if (hasModulithType(candidate, loader)) {
                return candidate.equals(packageName) ? packageName : child;
            }
            child = candidate;
        }
        return packageName;
    }

    /**
     * @return the direct sub-package of the base package whose {@code @ApplicationModule} declares the identifier,
     *         else the sub-package named like it
     * @throws IllegalStateException if neither exists
     */
    static String modulePackageById(String basePackage, String moduleId, ClassLoader loader) {
        List<String> subPackages = subPackages(basePackage, loader);
        Map<String, String> packagesById = new HashMap<>();
        subPackages.forEach(it -> packagesById.putIfAbsent(moduleId(it, loader), it));
        return Optional.ofNullable(packagesById.get(moduleId))
                .or(() -> Optional.of(basePackage + "." + moduleId).filter(subPackages::contains))
                .orElseThrow(() -> new IllegalStateException("The application in %s has no module %s. Known modules: %s"
                        .formatted(basePackage, moduleId, new TreeSet<>(packagesById.keySet()))));
    }

    private static String moduleId(String modulePackage, ClassLoader loader) {
        return moduleAnnotation(modulePackage, loader).map(ApplicationModule::id)
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> modulePackage.substring(modulePackage.lastIndexOf('.') + 1));
    }

    static Optional<String> basePackage(String packageName, ClassLoader loader) {
        for (String candidate = packageName; !candidate.isEmpty(); candidate = parent(candidate)) {
            if (hasModulithType(candidate, loader)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * @return the bean classes below the package, in the main or test output, sorted by name, without the classes of
     *         nested modules
     */
    static List<Class<?>> beanClasses(String modulePackage, ClassLoader loader) {
        Map<String, Boolean> nestedModules = new HashMap<>();
        List<Class<?>> beans = new ArrayList<>();
        for (String className : classNames(modulePackage, loader)) {
            if (inNestedModule(parent(className), modulePackage, loader, nestedModules)) {
                continue;
            }
            index(className, loader).filter(it -> isBean(it, loader))
                    .flatMap(it -> load(className, loader))
                    .ifPresent(beans::add);
        }
        return beans;
    }

    private static boolean inNestedModule(String packageName, String modulePackage, ClassLoader loader,
            Map<String, Boolean> nestedModules) {
        for (String candidate = packageName; candidate.length() > modulePackage.length(); candidate = parent(
                candidate)) {
            if (nestedModules.computeIfAbsent(candidate, it -> declaresModule(it, loader))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether ArC would make the class a bean in the application
     */
    static boolean isBean(ClassInfo type, ClassLoader loader) {
        return classify(type, loader) != ComponentKind.NONE;
    }

    /**
     * @return why the application would make the class a bean, following ArC's rules and the extension's
     */
    static ComponentKind classify(ClassInfo type, ClassLoader loader) {
        if (type.isInterface() || Modifier.isAbstract(type.flags()) || isVetoed(type, loader)) {
            return ComponentKind.NONE;
        }
        if (type.declaredAnnotations().stream().anyMatch(it -> isBeanDefining(it.name().toString(), loader))) {
            return ComponentKind.DECLARED;
        }
        if (!type.annotations(LISTENER).isEmpty()) {
            return ComponentKind.LISTENER;
        }
        boolean producesOrObserves = Stream.of(PRODUCES, OBSERVES, OBSERVES_ASYNC)
                .anyMatch(it -> !type.annotations(it).isEmpty());
        return producesOrObserves ? ComponentKind.PRODUCER_OR_OBSERVER : ComponentKind.NONE;
    }

    private static boolean isVetoed(ClassInfo type, ClassLoader loader) {
        return type.hasDeclaredAnnotation(VETOED) || load(type.name().packagePrefix() + ".package-info", loader)
                .filter(it -> it.isAnnotationPresent(Vetoed.class)).isPresent();
    }

    private static boolean isBeanDefining(String annotation, ClassLoader loader) {
        return load(annotation, loader).map(it -> it.isAnnotationPresent(Scope.class)
                || it.isAnnotationPresent(NormalScope.class) || it.isAnnotationPresent(Stereotype.class))
                .orElse(false);
    }

    /**
     * @return the names of the classes below the package, recursively, sorted
     */
    static TreeSet<String> classNames(String modulePackage, ClassLoader loader) {
        return classNames(modulePackage, loader, true);
    }

    /**
     * Reads directories ({@code file} URLs) and jars. Other class path locations are skipped.
     *
     * @param recursive whether to include sub-packages
     * @throws UncheckedIOException if a class path location cannot be read
     */
    static TreeSet<String> classNames(String modulePackage, ClassLoader loader, boolean recursive) {
        String path = modulePackage.replace('.', '/');
        TreeSet<String> names = new TreeSet<>();
        try {
            for (URL url : Collections.list(loader.getResources(path))) {
                found(url, path, modulePackage, recursive).stream()
                        .filter(it -> recursive || it.lastIndexOf('.') == modulePackage.length())
                        .forEach(names::add);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the classes of " + modulePackage, e);
        }
        return names;
    }

    private static List<String> found(URL url, String path, String modulePackage, boolean recursive)
            throws IOException {
        return switch (url.getProtocol()) {
            case JAR_PROTOCOL -> fromJar(url, path);
            case FILE_PROTOCOL -> fromDirectory(toPath(url), modulePackage, recursive);
            default -> {
                LOG.debugf("Skipping %s while listing the classes of %s: only directories and jars are read", url,
                        modulePackage);
                yield List.of();
            }
        };
    }

    private static Path toPath(URL url) throws IOException {
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid class path location " + url, e);
        }
    }

    private static List<String> subPackages(String basePackage, ClassLoader loader) {
        TreeSet<String> packages = new TreeSet<>();
        for (String className : classNames(basePackage, loader)) {
            String relative = className.substring(basePackage.length() + 1);
            if (relative.contains(".")) {
                packages.add(basePackage + "." + relative.substring(0, relative.indexOf('.')));
            }
        }
        return List.copyOf(packages);
    }

    private static boolean declaresModule(String packageName, ClassLoader loader) {
        return moduleAnnotation(packageName, loader).isPresent();
    }

    private static Optional<ApplicationModule> moduleAnnotation(String packageName, ClassLoader loader) {
        return load(packageName + ".package-info", loader)
                .map(it -> it.getAnnotation(ApplicationModule.class));
    }

    private static boolean hasModulithType(String packageName, ClassLoader loader) {
        return classNames(packageName, loader, false).stream()
                .flatMap(it -> load(it, loader).stream())
                .anyMatch(it -> it.isAnnotationPresent(Modulith.class) || it.isAnnotationPresent(Modulithic.class));
    }

    private static String parent(String packageName) {
        int dot = packageName.lastIndexOf('.');
        return dot < 0 ? "" : packageName.substring(0, dot);
    }

    private static List<String> fromJar(URL url, String path) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            return jar.stream().map(JarEntry::getName).filter(it -> it.startsWith(path + "/"))
                    .filter(ApplicationModuleComponents::isClassFile)
                    .map(it -> it.substring(0, it.length() - CLASS_SUFFIX.length()).replace('/', '.'))
                    .toList();
        }
    }

    private static List<String> fromDirectory(Path directory, String modulePackage, boolean recursive)
            throws IOException {
        try (Stream<Path> files = Files.walk(directory, recursive ? Integer.MAX_VALUE : 1)) {
            return files.map(it -> directory.relativize(it).toString().replace('\\', '/'))
                    .filter(ApplicationModuleComponents::isClassFile)
                    .map(it -> modulePackage + "." + it.substring(0, it.length() - CLASS_SUFFIX.length()).replace('/', '.'))
                    .toList();
        }
    }

    private static boolean isClassFile(String name) {
        return name.endsWith(CLASS_SUFFIX) && !name.endsWith("package-info.class")
                && !name.endsWith("module-info.class");
    }

    /**
     * @return the Jandex view of the class, empty if its class file cannot be read
     */
    static Optional<ClassInfo> index(String className, ClassLoader loader) {
        try (InputStream in = loader.getResourceAsStream(className.replace('.', '/') + CLASS_SUFFIX)) {
            Indexer indexer = new Indexer();
            indexer.index(in);
            return indexer.complete().getKnownClasses().stream().findFirst();
        } catch (IOException | RuntimeException e) {
            LOG.debugf(e, "Leaving %s out of the module slice: its class file cannot be read", className);
            return Optional.empty();
        }
    }

    private static Optional<Class<?>> load(String className, ClassLoader loader) {
        try {
            return Optional.of(Class.forName(className, false, loader));
        } catch (ClassNotFoundException | LinkageError e) {
            LOG.debugf(e, "Cannot load %s", className);
            return Optional.empty();
        }
    }
}
