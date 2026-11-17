package io.quarkiverse.modulith.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy;
import io.quarkiverse.modulith.core.ApplicationModuleSource;
import io.quarkiverse.modulith.core.ApplicationModuleSourceFactory;
import io.quarkiverse.modulith.core.JavaPackage;
import io.quarkiverse.modulith.core.JavaPackages;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.PackageName;
import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.TypeData;

/**
 * Builds the module model of an application from its class files. {@code ApplicationModules.of} uses it in plain tests
 * and the Quarkus build step uses it at build time, so both see the same model.
 */
public final class ModelAnalyzer {

    static final String DIRECT_SUB_PACKAGES = "direct-sub-packages";
    static final String EXPLICITLY_ANNOTATED = "explicitly-annotated";
    static final String DUPLICATE_IDENTIFIER = "Module identifier '%s' is used by the packages %s. Give each module a"
            + " unique identifier with @ApplicationModule(id = ...).";

    private static final Set<String> DECLARATIONS = Stream.of(Names.APPLICATION_MODULE, Names.NAMED_INTERFACE,
            Names.MODULITH, Set.of(Names.JMOLECULES_MODULE)).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());

    private ModelAnalyzer() {
    }

    /**
     * @param settings what to analyse and how
     * @param files the class files of the application
     * @return the model, not verified yet, and the warnings to show the user
     */
    public static AnalysisResult analyze(AnalysisSettings settings, ClassFiles files) {
        List<String> warnings = new ArrayList<>();
        Function<String, Optional<byte[]>> library = name -> readClass(settings.classLoader(), name);
        TypePool lookup = new TypePool(Map.of(), name -> files.read(name).or(() -> library.apply(name)));
        Optional<AnnotationData> modulith = settings.modulithType().flatMap(lookup::get)
                .flatMap(type -> type.annotations().stream().filter(it -> Names.MODULITH.contains(it.type()))
                        .filter(it -> settings.springAnnotations() || !it.type().startsWith(Names.SPRING_MODULITH))
                        .findFirst());
        String basePackage = settings.basePackage();
        String systemName = settings.systemName().orElseGet(() -> modulith.map(it -> it.value("systemName")
                .filter(name -> !name.isBlank())
                .orElseGet(() -> DependencyAnalysis.simpleName(settings.modulithType().orElseThrow()))).orElse(""));
        List<String> sharedModules = Stream.concat(values(modulith, "sharedModules"), settings.sharedModules().stream())
                .distinct().toList();
        boolean fullyQualified = settings.useFullyQualifiedModuleNames()
                .orElseGet(() -> modulith.flatMap(it -> it.value("useFullyQualifiedModuleNames")).filter("true"::equals)
                        .isPresent());
        PackageName base = PackageName.of(basePackage);
        List<String> basePackages = Stream.concat(Stream.of(basePackage), Stream.concat(
                values(modulith, "additionalPackages"), settings.additionalPackages().stream())
                .filter(it -> !base.contains(PackageName.of(it))))
                .distinct().toList();
        ApplicationModuleDetectionStrategy strategy = strategy(settings, warnings);
        List<ApplicationModuleSourceFactory> factories = ServiceLoader
                .load(ApplicationModuleSourceFactory.class, settings.classLoader()).stream()
                .map(ServiceLoader.Provider::get).toList();
        List<String> rootPackages = Stream.concat(basePackages.stream(),
                factories.stream().flatMap(it -> it.getRootPackages().stream())).distinct().toList();

        Map<String, ScannedClass> scanned = scan(settings, files, rootPackages);
        TypePool pool = new TypePool(scanned, library);
        TypeFacts facts = new TypeFacts(pool);
        Map<String, TypeData> types = new TreeMap<>();
        scanned.values().forEach(it -> types.put(it.name(), typeData(it, pool)));
        List<JavaType> javaTypes = types.values().stream().map(JavaType::of).toList();

        List<ApplicationModuleSource> sources = sources(javaTypes, basePackages, strategy, factories, fullyQualified);
        JavaPackages exclusions = new JavaPackages(sources.stream().map(ApplicationModuleSource::getModuleBasePackage)
                .toList());
        Map<String, String> moduleOfType = new HashMap<>();
        List<ModuleSkeleton> skeletons = sources.stream().map(source -> {
            JavaPackage basePkg = source.getModuleBasePackage().without(exclusions);
            basePkg.getTypes().forEach(it -> moduleOfType.put(it.getName(), source.getIdentifier().toString()));
            return new ModuleSkeleton(source, basePkg);
        }).toList();

        ModuleAnalysis analysis = new ModuleAnalysis(scanned, facts,
                new DependencyAnalysis(pool, facts, moduleOfType, settings.allowFieldInjection()),
                new EventAnalysis(pool), types, moduleOfType, exclusions);
        List<ModuleData> modules = skeletons.stream().map(analysis::module)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
        scanned.values().stream().filter(it -> !moduleOfType.containsKey(it.name())).forEach(it -> types.put(it.name(),
                facts.classified(types.get(it.name()), it, facts.isBean(it) ? List.of(it.name()) : List.of(),
                        List.of())));
        ModelData data = new ModelData(systemName, basePackage, modules, List.of(), "",
                settings.modulithType().orElse(basePackage), rootPackages, sharedModules, fullyQualified,
                List.copyOf(types.values()), false);
        return new AnalysisResult(data, List.copyOf(warnings));
    }

    private static Stream<String> values(Optional<AnnotationData> annotation, String attribute) {
        return annotation.flatMap(it -> it.values(attribute)).stream().flatMap(List::stream);
    }

    /**
     * @return the configured strategy, else the one registered as service, else the direct sub-packages strategy
     */
    static ApplicationModuleDetectionStrategy strategy(AnalysisSettings settings, List<String> warnings) {
        return switch (settings.detectionStrategy()) {
            case "" -> registeredStrategy(settings.classLoader(), warnings);
            case DIRECT_SUB_PACKAGES -> ApplicationModuleDetectionStrategy.directSubPackage();
            case EXPLICITLY_ANNOTATED -> ApplicationModuleDetectionStrategy.explicitlyAnnotated();
            default -> instantiate(settings.detectionStrategy(), settings.classLoader());
        };
    }

    private static ApplicationModuleDetectionStrategy registeredStrategy(ClassLoader classLoader, List<String> warnings) {
        List<ApplicationModuleDetectionStrategy> registered = ServiceLoader
                .load(ApplicationModuleDetectionStrategy.class, classLoader).stream()
                .map(ServiceLoader.Provider::get).toList();
        if (registered.size() > 1) {
            throw new IllegalStateException(
                    "Multiple module detection strategies configured. Only one supported! %s".formatted(registered));
        }
        registered.forEach(it -> warnings.add(("Registering the module detection strategy %s in META-INF/services is"
                + " deprecated! Set quarkus.modulith.detection-strategy to its class name instead.")
                .formatted(it.getClass().getName())));
        return registered.stream().findFirst().orElseGet(ApplicationModuleDetectionStrategy::directSubPackage);
    }

    private static ApplicationModuleDetectionStrategy instantiate(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader).asSubclass(ApplicationModuleDetectionStrategy.class)
                    .getConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException(("Cannot use %s as module detection strategy. Set"
                    + " quarkus.modulith.detection-strategy to direct-sub-packages, explicitly-annotated or the name of"
                    + " an ApplicationModuleDetectionStrategy with a public no-argument constructor.").formatted(name), e);
        }
    }

    private static Map<String, ScannedClass> scan(AnalysisSettings settings, ClassFiles files, List<String> rootPackages) {
        List<PackageName> roots = rootPackages.stream().map(PackageName::of).toList();
        List<PackagePattern> ignored = settings.ignoredPackages().stream().map(PackagePattern::of).toList();
        Map<String, ScannedClass> result = new TreeMap<>();
        files.names(rootPackages).stream()
                .filter(name -> roots.stream().anyMatch(it -> it.contains(PackageName.ofType(name))))
                .filter(name -> ignored.stream().noneMatch(it -> it.matches(PackageName.ofType(name).getName())))
                .filter(name -> !settings.ignoredClasses().test(name))
                .forEach(name -> files.read(name).map(ClassFileScanner::scan)
                        .map(it -> settings.springAnnotations() ? it : SpringAnnotations.strip(it))
                        .ifPresent(it -> result.put(name, it)));
        return result;
    }

    private static TypeData typeData(ScannedClass type, TypePool pool) {
        Set<String> annotations = type.annotations().stream().map(AnnotationData::type)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> metaAnnotations = pool.metaAnnotations(type.annotations()).stream()
                .filter(it -> !annotations.contains(it)).toList();
        List<AnnotationData> declarations = Stream.concat(
                type.annotations().stream().filter(it -> DECLARATIONS.contains(it.type())),
                pool.declaration(type.annotations(), Names.NAMED_INTERFACE).stream()
                        .filter(it -> !annotations.contains(it.type())))
                .toList();
        List<String> signatureTypes = Hierarchy.of(pool, type)
                .flatMap(it -> it.methods().stream())
                .filter(ScannedMethod::isPublic)
                .flatMap(it -> it.involvedTypes().stream())
                .filter(pool::isApplicationClass)
                .distinct().toList();
        return new TypeData(type.name(), type.access(), List.copyOf(pool.supertypes(type.name())), type.interfaces(),
                List.copyOf(annotations), metaAnnotations, signatureTypes, declarations, List.of(), List.of());
    }

    private static List<ApplicationModuleSource> sources(List<JavaType> types, List<String> basePackages,
            ApplicationModuleDetectionStrategy strategy, List<ApplicationModuleSourceFactory> factories,
            boolean fullyQualified) {
        Function<String, JavaPackage> packages = name -> JavaPackage.of(types, name);
        Stream<ApplicationModuleSource> direct = basePackages.stream()
                .flatMap(it -> ApplicationModuleSource.from(packages.apply(it), strategy, fullyQualified));
        Stream<ApplicationModuleSource> contributed = factories.stream().flatMap(factory -> Stream.concat(
                factory.getRootPackages().stream().flatMap(it -> factory.getApplicationModuleSources(packages.apply(it),
                        Objects.requireNonNullElse(factory.getApplicationModuleDetectionStrategy(), strategy),
                        fullyQualified)),
                factory.getApplicationModuleSources(packages, fullyQualified)));
        List<ApplicationModuleSource> result = Stream.concat(direct, contributed).distinct().toList();
        String duplicates = result.stream().collect(Collectors.groupingBy(it -> it.getIdentifier().toString(),
                TreeMap::new, Collectors.mapping(it -> it.getModuleBasePackage().getName(), Collectors.toList())))
                .entrySet().stream().filter(it -> it.getValue().size() > 1)
                .map(it -> DUPLICATE_IDENTIFIER.formatted(it.getKey(), it.getValue()))
                .collect(Collectors.joining("\n"));
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException(duplicates);
        }
        return result;
    }

    /**
     * @return the type of the given package or its sub-packages annotated with {@code @Modulith} or
     *         {@code @Modulithic}, if there is exactly one
     */
    public static Optional<String> findModulithType(ClassFiles files, String basePackage, boolean springAnnotations) {
        List<String> annotated = files.names(List.of(basePackage)).stream()
                .filter(name -> files.read(name).map(ClassFileScanner::annotations).orElse(List.of()).stream()
                        .filter(Names.MODULITH::contains)
                        .anyMatch(it -> springAnnotations || !it.startsWith(Names.SPRING_MODULITH)))
                .toList();
        return annotated.size() == 1 ? Optional.of(annotated.get(0)) : Optional.empty();
    }

    static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static Optional<byte[]> readClass(ClassLoader classLoader, String name) {
        return Optional.ofNullable(classLoader.getResourceAsStream(name.replace('.', '/') + ".class"))
                .map(ModelAnalyzer::readAll);
    }

    static byte[] readAll(InputStream input) {
        try (input) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
