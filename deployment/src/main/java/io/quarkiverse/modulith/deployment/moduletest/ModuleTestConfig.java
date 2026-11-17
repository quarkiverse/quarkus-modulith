package io.quarkiverse.modulith.deployment.moduletest;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Module integration tests. These properties are usually set by {@code ApplicationModuleTestProfile} and not by hand.
 * <p>
 * The prefix is shared by phase: this root holds the build-time properties, {@code ModuleTestRuntimeConfig} of the
 * runtime module the run-time ones ({@code thread-bound-published-events}). The properties of
 * {@code quarkus-modulith-junit-changes} are declared here too, so that Quarkus documents them and does not report
 * them as unknown, although only that library reads them, before Quarkus starts.
 */
@ConfigMapping(prefix = "quarkus.modulith.test")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ModuleTestConfig {

    /**
     * The module under test, given as a module identifier or as any package inside the module. When set, beans of
     * modules that are not part of the test are excluded from the application.
     */
    Optional<String> module();

    /**
     * Which other modules are started together with the module under test.
     */
    @WithDefault("standalone")
    BootstrapMode bootstrapMode();

    /**
     * Additional modules to start, by identifier.
     */
    Optional<List<String>> extraModules();

    /**
     * Whether a module test build fails on any module rule violation of the application, also when
     * {@code quarkus.modulith.verification.mode} is {@code warn}. Only used when {@code module} is set.
     */
    @WithDefault("true")
    boolean verifyAutomatically();

    /**
     * Read by {@code quarkus-modulith-junit-changes}: whether to run all tests, without looking at the changed files.
     */
    @WithDefault("false")
    boolean skipOptimizations();

    /**
     * Read by {@code quarkus-modulith-junit-changes}: what to run when no file changed, {@code execute-all} (the
     * default) or {@code skip-all} ({@code execute-none}).
     */
    Optional<String> onNoChanges();

    /**
     * Read by {@code quarkus-modulith-junit-changes}: how changed files are found, {@code default},
     * {@code uncommitted-changes}, {@code reference-commit} or the name of a {@code FileModificationDetector} class.
     */
    Optional<String> fileModificationDetector();

    /**
     * Read by {@code quarkus-modulith-junit-changes}: the commit the {@code reference-commit} detector compares with.
     */
    Optional<String> referenceCommit();

    enum BootstrapMode {
        /**
         * Only the module under test and the shared modules.
         */
        STANDALONE,
        /**
         * The module under test and the modules it depends on directly.
         */
        DIRECT_DEPENDENCIES,
        /**
         * The module under test and all the modules it depends on, transitively.
         */
        ALL_DEPENDENCIES
    }
}
