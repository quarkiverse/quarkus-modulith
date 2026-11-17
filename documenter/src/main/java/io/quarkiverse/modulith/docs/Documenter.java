package io.quarkiverse.modulith.docs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;

/**
 * Writes the documentation of application modules, the port of Spring Modulith's {@code Documenter}:
 *
 * <pre>
 * new Documenter(ApplicationModules.of(Application.class))
 *         .writeModulesAsPlantUml()
 *         .writeIndividualModulesAsPlantUml()
 *         .writeModuleCanvases();
 * </pre>
 *
 * It writes C4 or UML PlantUML component diagrams of all modules ({@code components.puml}) and of each module with its
 * dependencies ({@code module-<id>.puml}), an Asciidoc canvas per module ({@code module-<id>.adoc}), an aggregating
 * document ({@code all-docs.adoc}) and the module metadata for runtime use. The output matches Spring Modulith's
 * except where {@code docs/modules/ROOT/pages/documentation.adoc} says otherwise.
 * <p>
 * Like Spring Modulith's, a documenter builds its diagram model once, on the first diagram it writes: the component
 * names of that call's {@link DiagramOptions#withDefaultDisplayName} and the colors of its
 * {@link DiagramOptions#withColorSelector} stay for every later diagram of the same documenter. Use a new documenter
 * for diagrams with other names or colors.
 * <p>
 * The Quarkus build writes the same files for the running application, see {@code quarkus.modulith.docs.*}.
 */
public class Documenter {

    /**
     * Where {@link #writeModuleMetadata()} writes the full module metadata, relative to the resource output folder.
     */
    public static final String MODULE_METADATA_LOCATION = "META-INF/quarkus-modulith/application-modules.json";

    static final String DEFAULT_COMPONENTS_FILE = "components.puml";
    static final String DEFAULT_MODULE_COMPONENTS_FILE = "module-%s.puml";
    static final String AGGREGATING_FILE = "all-docs.adoc";

    private static final List<Map.Entry<DependencyType, String>> DEPENDENCY_DESCRIPTIONS = List.of(
            Map.entry(DependencyType.EVENT_LISTENER, "listens to"),
            Map.entry(DependencyType.DEFAULT, "depends on"));

    private final Supplier<DocumentationModel> modelSource;
    private final DocumenterOptions options;
    private final Path resourceFolder;
    private DocumentationModel model;
    private DiagramModel diagram;
    private boolean cleared;

    /**
     * Documents the modules of the application whose base package is the package of the given type, written to
     * {@link DocumenterOptions#DEFAULT_LOCATION}. The modules are built by {@link ApplicationModules#of(Class)} when the
     * documenter first needs them.
     */
    public Documenter(Class<?> modulithType) {
        this(() -> DocumentationModel.of(ApplicationModules.of(modulithType)), DocumenterOptions.defaults());
    }

    /**
     * @param modules the modules to document, written to {@link DocumenterOptions#DEFAULT_LOCATION}
     */
    public Documenter(ApplicationModules modules) {
        this(modules, DocumenterOptions.defaults());
    }

    /**
     * @param modules the modules to document, see {@link DocumentationModel#of(ApplicationModules)}
     * @param options where to write
     */
    public Documenter(ApplicationModules modules, DocumenterOptions options) {
        this(DocumentationModel.of(Objects.requireNonNull(modules, "modules")), options);
    }

    /**
     * @param model the modules with their details
     * @param options where to write
     */
    public Documenter(DocumentationModel model, DocumenterOptions options) {
        this(constant(model), options);
    }

    Documenter(DocumentationModel model, DocumenterOptions options, Path resourceFolder) {
        this(constant(model), options, resourceFolder);
    }

    private Documenter(Supplier<DocumentationModel> modelSource, DocumenterOptions options) {
        this(modelSource, options, Path.of(resourceFolder(Path.of(""))));
    }

    private Documenter(Supplier<DocumentationModel> modelSource, DocumenterOptions options, Path resourceFolder) {
        this.modelSource = modelSource;
        this.options = Objects.requireNonNull(options, "options");
        this.resourceFolder = resourceFolder;
    }

    private static Supplier<DocumentationModel> constant(DocumentationModel model) {
        Objects.requireNonNull(model, "model");
        return () -> model;
    }

    private DocumentationModel model() {
        if (Objects.isNull(model)) {
            model = modelSource.get();
        }
        return model;
    }

    /**
     * @return the class output folder of the build: {@code target/classes} for Maven, {@code build/resources/main}
     *         otherwise
     */
    static String resourceFolder(Path projectFolder) {
        return Files.exists(projectFolder.resolve("pom.xml")) ? "target/classes" : "build/resources/main";
    }

    /**
     * @return the modules this documenter writes about
     */
    public ApplicationModules getModules() {
        return model().modules();
    }

    /**
     * @return the modules with their details
     */
    public DocumentationModel getModel() {
        return model();
    }

    /**
     * Writes all diagrams, the module canvases, the aggregating document and the module metadata, with the default
     * options, see {@link #writeDocumentation(DiagramOptions, CanvasOptions)}.
     *
     * @return this documenter
     */
    public Documenter writeDocumentation() {
        return writeDocumentation(DiagramOptions.defaults(), CanvasOptions.defaults());
    }

    /**
     * Writes all diagrams, the module canvases, the aggregating document and the module metadata. The module metadata
     * goes to the resource output folder on disk even when {@link DocumenterOptions#withOutput} names another output,
     * see {@link #writeModuleMetadata()}.
     *
     * @return this documenter
     */
    public Documenter writeDocumentation(DiagramOptions diagramOptions, CanvasOptions canvasOptions) {
        potentiallyWipeOutput();
        return writeModulesAsPlantUml(diagramOptions)
                .writeIndividualModulesAsPlantUml(diagramOptions)
                .writeModuleCanvases(canvasOptions)
                .writeAggregatingDocument(diagramOptions, canvasOptions)
                .writeModuleMetadata();
    }

    /**
     * Writes {@code all-docs.adoc}, which includes the diagrams and canvases written before, with the default options.
     *
     * @return this documenter
     */
    public Documenter writeAggregatingDocument() {
        return writeAggregatingDocument(DiagramOptions.defaults(), CanvasOptions.defaults());
    }

    /**
     * Writes {@code all-docs.adoc}, which includes the diagrams and canvases written before with the given options.
     *
     * @return this documenter
     */
    public Documenter writeAggregatingDocument(DiagramOptions diagramOptions, CanvasOptions canvasOptions) {
        Objects.requireNonNull(diagramOptions, "diagramOptions");
        Objects.requireNonNull(canvasOptions, "canvasOptions");
        potentiallyWipeOutput();
        DocumentationOutput output = options.output();
        String componentsFile = diagramOptions.targetFileName().orElse(DEFAULT_COMPONENTS_FILE);
        StringBuilder document = new StringBuilder();
        if (output.contains(componentsFile)) {
            document.append(Asciidoctor.renderHeadline(2, model().systemName()))
                    .append(Asciidoctor.renderPlantUmlInclude(componentsFile))
                    .append(Asciidoctor.NL);
        }
        for (ApplicationModule module : model().modules()) {
            String diagramFile = diagramOptions.targetFileName().orElse(DEFAULT_MODULE_COMPONENTS_FILE)
                    .formatted(module.getName());
            String canvasFile = canvasOptions.targetFileName(module.getName());
            StringBuilder content = new StringBuilder()
                    .append(output.contains(diagramFile) ? Asciidoctor.renderPlantUmlInclude(diagramFile) : "")
                    .append(output.contains(canvasFile) ? Asciidoctor.renderGeneralInclude(canvasFile) : "");
            if (!content.isEmpty()) {
                document.append(Asciidoctor.renderHeadline(2, module.getDisplayName())).append(content)
                        .append(Asciidoctor.NL);
            }
        }
        if (!document.toString().isBlank()) {
            output.write(AGGREGATING_FILE, document.toString());
        }
        return this;
    }

    /**
     * Writes the component diagram of all modules to {@code components.puml}, with the default options.
     *
     * @return this documenter
     */
    public Documenter writeModulesAsPlantUml() {
        return writeModulesAsPlantUml(DiagramOptions.defaults());
    }

    /**
     * Writes the component diagram of all modules to {@code components.puml}.
     *
     * @return this documenter
     */
    public Documenter writeModulesAsPlantUml(DiagramOptions diagramOptions) {
        Objects.requireNonNull(diagramOptions, "diagramOptions");
        potentiallyWipeOutput();
        options.output().write(diagramOptions.targetFileName().orElse(DEFAULT_COMPONENTS_FILE),
                toPlantUml(diagramOptions));
        return this;
    }

    /**
     * Writes one diagram per module, with its dependencies, to {@code module-<id>.puml}, with the default options.
     *
     * @return this documenter
     */
    public Documenter writeIndividualModulesAsPlantUml() {
        return writeIndividualModulesAsPlantUml(DiagramOptions.defaults());
    }

    /**
     * Writes one diagram per module, with its dependencies, to {@code module-<id>.puml}.
     *
     * @return this documenter
     */
    public Documenter writeIndividualModulesAsPlantUml(DiagramOptions diagramOptions) {
        Objects.requireNonNull(diagramOptions, "diagramOptions");
        potentiallyWipeOutput();
        model().modules().forEach(module -> writeModuleAsPlantUml(module, diagramOptions));
        return this;
    }

    /**
     * Writes the diagram of the given module, with the default options.
     *
     * @return this documenter
     */
    public Documenter writeModuleAsPlantUml(ApplicationModule module) {
        return writeModuleAsPlantUml(module, DiagramOptions.defaults());
    }

    /**
     * Writes the diagram of the given module and its dependencies to {@code module-<id>.puml}.
     *
     * @return this documenter
     */
    public Documenter writeModuleAsPlantUml(ApplicationModule module, DiagramOptions diagramOptions) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(diagramOptions, "diagramOptions");
        potentiallyWipeOutput();
        DiagramView view = new DiagramView(diagram(diagramOptions),
                Optional.of(diagramOptions.defaultDisplayName().apply(module)));
        List<ApplicationModule> dependencies = new ArrayList<>(
                model().bootstrapDependencies(module, diagramOptions.dependencyDepth()));
        diagramOptions.dependencyTypes().forEach(type -> dependencies.addAll(model().directDependencies(module, type)));
        addComponentsToView(dependencies, view, diagramOptions, it -> it.add(diagram.component(module)));
        String fileName = diagramOptions.targetFileName().orElse(DEFAULT_MODULE_COMPONENTS_FILE)
                .formatted(module.getName());
        options.output().write(fileName, PlantUml.render(view, diagramOptions.style(), diagramOptions.skinParams()));
        return this;
    }

    /**
     * Writes one canvas per module to {@code module-<id>.adoc}, with the default options.
     *
     * @return this documenter
     */
    public Documenter writeModuleCanvases() {
        return writeModuleCanvases(CanvasOptions.defaults());
    }

    /**
     * Writes one canvas per module to {@code module-<id>.adoc}.
     *
     * @return this documenter
     */
    public Documenter writeModuleCanvases(CanvasOptions canvasOptions) {
        Objects.requireNonNull(canvasOptions, "canvasOptions");
        potentiallyWipeOutput();
        model().modules().forEach(module -> options.output().write(canvasOptions.targetFileName(module.getName()),
                toModuleCanvas(module, canvasOptions)));
        return this;
    }

    /**
     * Writes the module metadata, as {@link ApplicationModulesJson#full(DocumentationModel)}, to
     * {@value #MODULE_METADATA_LOCATION} in the resource output folder of the build ({@code target/classes} with
     * Maven), so that it is packaged with the application. It ignores {@link DocumenterOptions#withOutput} and the
     * output folder.
     *
     * @return this documenter
     */
    public Documenter writeModuleMetadata() {
        new FolderOutput(resourceFolder).write(MODULE_METADATA_LOCATION, ApplicationModulesJson.full(model()));
        return this;
    }

    String toPlantUml(DiagramOptions diagramOptions) {
        DiagramView view = new DiagramView(diagram(diagramOptions), Optional.of(model().systemName()));
        List<ApplicationModule> topLevel = model().modules().stream().filter(it -> !model().hasParent(it)).toList();
        addComponentsToView(topLevel, view, diagramOptions, it -> {
        });
        return PlantUml.render(view, diagramOptions.style(), diagramOptions.skinParams());
    }

    String toModuleCanvas(ApplicationModule module, CanvasOptions canvasOptions) {
        Asciidoctor asciidoctor = new Asciidoctor(model(), canvasOptions.apiBase());
        ModuleDetails details = model().details(module);
        List<ModuleType> aggregates = details.aggregateRoots().stream().filter(canvasOptions::shows).toList();
        List<ModuleType> valueTypes = details.valueTypes().stream().filter(canvasOptions::shows).toList();
        Function<List<ModuleType>, String> mapper = asciidoctor::typesToBulletPoints;
        return new StringBuilder()
                .append(Asciidoctor.startTable("%autowidth.stretch, cols=\"h,a\""))
                .append(row("Description", asciidoctor.renderModuleDescription(module), canvasOptions))
                .append(row("Base package", asciidoctor.toInlineCode(module.getBasePackage()), canvasOptions))
                .append(row("Components", asciidoctor.renderBeans(details, canvasOptions), canvasOptions))
                .append(row("Bean references", asciidoctor.renderBeanReferences(module), canvasOptions))
                .append(row("Aggregate roots", canvasOptions, aggregates, mapper))
                .append(row("Value types", canvasOptions, valueTypes, mapper))
                .append(row("Published events", asciidoctor.renderPublishedEvents(module), canvasOptions))
                .append(row("Events listened to", asciidoctor.renderEventsListenedTo(module), canvasOptions))
                .append(row("Properties", asciidoctor.renderConfigurationProperties(details.properties()),
                        canvasOptions))
                .append(Asciidoctor.startOrEndTable())
                .toString();
    }

    private DiagramModel diagram(DiagramOptions diagramOptions) {
        if (Objects.isNull(diagram)) {
            diagram = new DiagramModel(model().systemName());
            for (ApplicationModule module : model().modules()) {
                diagram.addComponent(module, diagramOptions.defaultDisplayName().apply(module));
            }
            model().modules().forEach(this::addDependencies);
        }
        return diagram;
    }

    private void addDependencies(ApplicationModule module) {
        DiagramComponent component = diagram.component(module);
        DEPENDENCY_DESCRIPTIONS.forEach(entry -> model().directDependencies(module, entry.getKey())
                .forEach(target -> diagram.uses(component, diagram.component(target), entry.getValue(),
                        entry.getKey().name())));
        model().bootstrapDependencies(module, DependencyDepth.IMMEDIATE)
                .forEach(target -> diagram.uses(component, diagram.component(target), "uses",
                        DependencyType.USES_COMPONENT.name()));
    }

    private void addComponentsToView(List<ApplicationModule> modules, DiagramView view, DiagramOptions diagramOptions,
            Consumer<DiagramView> afterCleanup) {
        for (ApplicationModule module : new LinkedHashSet<>(modules)) {
            if (diagramOptions.exclusions().test(module)) {
                continue;
            }
            DiagramComponent component = diagram.component(module);
            diagramOptions.colorSelector().apply(module).ifPresent(color -> diagram.color(component, color));
            if (diagramOptions.componentFilter().test(component)) {
                view.add(component);
            }
        }
        EnumSet.complementOf(diagramOptions.dependencyTypes()).forEach(type -> view.removeRelationshipsWithTag(type.name()));
        afterCleanup.accept(view);
        modules.stream().filter(diagramOptions.targetOnly())
                .forEach(module -> view.removeRelationshipsFrom(diagram.component(module)));
        if (diagramOptions.hideElementsWithoutRelationships()) {
            view.removeElementsWithNoRelationships();
        }
        afterCleanup.accept(view);
        view.relationships().stream()
                .collect(Collectors.groupingBy(it -> List.of(it.source(), it.destination()), LinkedHashMap::new,
                        Collectors.toList()))
                .values().stream()
                .filter(it -> it.size() > 1)
                .forEach(it -> it.stream().filter(relationship -> relationship.tag().equals(DependencyType.DEFAULT.name()))
                        .findFirst().ifPresent(view::remove));
    }

    private void potentiallyWipeOutput() {
        if (options.clean() && !cleared) {
            options.output().clear();
            cleared = true;
        }
    }

    private static String row(String title, String content, CanvasOptions options) {
        return options.hideEmptyLines() && (content.isBlank() || content.equalsIgnoreCase("none"))
                ? ""
                : Asciidoctor.writeTableRow(title, content);
    }

    private static <T> String row(String title, CanvasOptions options, List<T> items, Function<List<T>, String> mapper) {
        return options.hideEmptyLines() && items.isEmpty() ? "" : Asciidoctor.writeTableRow(title, mapper.apply(items));
    }
}
