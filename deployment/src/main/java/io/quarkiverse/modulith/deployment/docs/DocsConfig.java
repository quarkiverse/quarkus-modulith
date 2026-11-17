package io.quarkiverse.modulith.deployment.docs;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.DiagramStyle;
import io.quarkiverse.modulith.docs.ElementsWithoutRelationships;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Generated module documentation.
 */
@ConfigMapping(prefix = "quarkus.modulith.docs")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DocsConfig {

    /**
     * Whether to write the module documentation (PlantUML and Mermaid diagrams, module canvases, aggregating document
     * and overview) when the application is packaged.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether to write the documentation in dev mode too, on every reload. Off by default to keep reloads fast.
     */
    @WithDefault("false")
    boolean devMode();

    /**
     * The directory the documentation is written to. Relative paths are resolved against the build output directory
     * ({@code target} with Maven). Defaults to {@code modulith-docs}. It must name a sub-directory of the build output
     * of its own: an absolute path outside the build output, {@code .}, a path that leaves the build output with
     * {@code ..} or a path inside a directory of the build such as {@code classes} or {@code quarkus-app} fails the
     * build, because the directory is cleaned.
     */
    @WithDefault("modulith-docs")
    String outputDirectory();

    /**
     * Whether to delete the content of the output directory before writing, so files of renamed or removed modules
     * do not linger. Spring Modulith's {@code Documenter} cleans by default too.
     */
    @WithDefault("true")
    boolean clean();

    /**
     * The PlantUML diagrams.
     */
    Diagram diagram();

    /**
     * The module canvases.
     */
    Canvas canvas();

    interface Diagram {

        /**
         * The diagram style: {@code c4} component diagrams or plain {@code uml} ones.
         */
        @WithDefault("c4")
        DiagramStyle style();

        /**
         * How far the diagram of a module follows the modules whose beans it uses.
         */
        @WithDefault("immediate")
        DependencyDepth dependencyDepth();

        /**
         * The dependency types drawn. All types when not set.
         */
        Optional<List<DependencyType>> dependencyTypes();

        /**
         * Whether modules without a relationship to another module of the diagram are shown.
         */
        @WithDefault("hidden")
        ElementsWithoutRelationships elementsWithoutRelationships();
    }

    interface Canvas {

        /**
         * The groups beans are listed in.
         */
        @WithDefault("quarkus")
        CanvasGroupings groupings();

        /**
         * Whether internal beans and types are listed as well.
         */
        @WithDefault("false")
        boolean revealInternals();

        /**
         * Whether rows without content are written.
         */
        @WithDefault("false")
        boolean revealEmptyLines();

        /**
         * The base URL of the application's Javadoc. When set, types link to their Javadoc page.
         */
        Optional<String> apiBase();
    }
}
