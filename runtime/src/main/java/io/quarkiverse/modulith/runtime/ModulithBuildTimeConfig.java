package io.quarkiverse.modulith.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration of Quarkus Modulith. The same properties apply to {@code ApplicationModules.of} in a plain
 * test, read from {@code application.properties}.
 */
@ConfigMapping(prefix = "quarkus.modulith")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ModulithBuildTimeConfig {

    /**
     * The base package of the application. Every direct sub-package is an application module. When not set, the
     * package of the type annotated with {@code @Modulith} is used, or else the longest package prefix shared by all
     * application classes.
     */
    Optional<String> basePackage();

    /**
     * How modules are found: {@code direct-sub-packages} (every direct sub-package of the base package is a module),
     * {@code explicitly-annotated} (only packages annotated with {@code @ApplicationModule} or jMolecules'
     * {@code @Module}), or the fully qualified name of an {@code ApplicationModuleDetectionStrategy} with a public
     * no-argument constructor. When not set, a strategy registered as service is used, else
     * {@code direct-sub-packages}.
     */
    Optional<String> detectionStrategy();

    /**
     * The name of the system, overriding {@code @Modulith(systemName)}.
     */
    Optional<String> systemName();

    /**
     * Identifiers of modules every other module may depend on, in addition to {@code @Modulith(sharedModules)}.
     */
    Optional<List<String>> sharedModules();

    /**
     * Further root packages whose sub-packages are modules, in addition to {@code @Modulith(additionalPackages)}.
     */
    Optional<List<String>> additionalPackages();

    /**
     * Whether module identifiers default to the full package name, overriding
     * {@code @Modulith(useFullyQualifiedModuleNames)}.
     */
    Optional<Boolean> useFullyQualifiedModuleNames();

    /**
     * Packages left out of the model, in ArchUnit's notation: {@code com.acme.legacy} for the package itself,
     * {@code com.acme.legacy..} for it and its sub-packages, {@code ..generated..} for any package with that segment.
     */
    Optional<List<String>> ignoredPackages();

    /**
     * Support for Spring Modulith's annotations.
     */
    SpringAnnotations springAnnotations();

    /**
     * Module verification.
     */
    Verification verification();

    /**
     * The export of the model in Spring Modulith's JSON format.
     */
    Export export();

    interface Export {

        /**
         * Whether the build writes the model in Spring Modulith's {@code application-modules.json} format to
         * {@code META-INF/quarkus-modulith/application-modules.json} in the application. Quarkus Modulith does not read
         * the file itself, so enable it only for tools that expect Spring Modulith's file.
         */
        @WithDefault("false")
        boolean enabled();
    }

    interface SpringAnnotations {

        /**
         * Whether {@code org.springframework.modulith} annotations ({@code @ApplicationModule}, {@code @NamedInterface},
         * {@code @Modulith}, {@code @Modulithic}, {@code @PackageInfo}, {@code @ApplicationModuleListener}) count like
         * Quarkus Modulith's own, so a code base can keep them while it migrates. The annotations are read from the
         * class files, Spring is not needed.
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface Verification {

        /**
         * What to do when the module rules are broken: {@code fail} the build, only log a {@code warn}ing, or skip
         * the verification ({@code disabled}).
         */
        @WithDefault("fail")
        Mode mode();

        /**
         * Whether test classes are part of the module model. Off by default, like in Spring Modulith, so that tests
         * placed in a module package may use the internals of other modules.
         */
        @WithDefault("false")
        boolean includeTestClasses();

        /**
         * Whether beans of other modules may be injected into fields. Spring Modulith reports field injection across
         * modules as violation and asks for constructor injection. Quarkus recommends package-private field injection,
         * so it is allowed by default. Set to {@code false} for Spring Modulith's rule.
         */
        @WithDefault("true")
        boolean allowFieldInjection();

        /**
         * Fully qualified names of {@code io.quarkiverse.modulith.core.ModulithRule} implementations with a public
         * no-argument constructor, run with the module rules.
         */
        Optional<List<String>> additionalRules();

        /**
         * Whether to run the jMolecules DDD and architecture rules of {@code jmolecules-archunit} at build time, like
         * Spring Modulith does by default in tests. Needs ArchUnit and {@code jmolecules-archunit} on the build class
         * path.
         */
        @WithDefault("false")
        boolean jmoleculesRules();

        enum Mode {
            FAIL,
            WARN,
            DISABLED
        }
    }
}
