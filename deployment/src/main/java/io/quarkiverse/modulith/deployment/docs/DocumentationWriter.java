package io.quarkiverse.modulith.deployment.docs;

import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.docs.ApplicationModulesJson;
import io.quarkiverse.modulith.docs.CanvasOptions;
import io.quarkiverse.modulith.docs.DiagramOptions;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.DocumentationOutput;
import io.quarkiverse.modulith.docs.Documenter;
import io.quarkiverse.modulith.docs.DocumenterOptions;

/**
 * Renders the module documentation into memory, file name to content:
 * <ul>
 * <li>{@code components.puml}, {@code module-<id>.puml}, {@code module-<id>.adoc} and {@code all-docs.adoc}: what
 * Spring Modulith's {@code Documenter.writeDocumentation()} writes, rendered by the {@link Documenter},</li>
 * <li>{@code components.mmd}: Mermaid diagram of all modules,</li>
 * <li>{@code modules.md}: an overview that renders on GitHub and GitLab,</li>
 * <li>{@code application-modules.json}: the module metadata in the format of Spring Modulith's
 * {@code ApplicationModulesExporter.toFullJson()}, a copy of the class path resource the build writes.</li>
 * </ul>
 */
public final class DocumentationWriter {

    static final String METADATA_FILE = "application-modules.json";

    private DocumentationWriter() {
    }

    public static Map<String, String> render(ModuleModel model, DocumentationModel documentation,
            DiagramOptions diagramOptions, CanvasOptions canvasOptions) {
        Map<String, String> files = new LinkedHashMap<>();
        DocumentationOutput output = new MapOutput(files);
        new Documenter(documentation, DocumenterOptions.defaults().withOutput(output))
                .writeModulesAsPlantUml(diagramOptions)
                .writeIndividualModulesAsPlantUml(diagramOptions)
                .writeModuleCanvases(canvasOptions)
                .writeAggregatingDocument(diagramOptions, canvasOptions);
        files.put("components.mmd", Diagrams.mermaid(model));
        files.put(METADATA_FILE, ApplicationModulesJson.full(documentation));
        files.put("modules.md", overview(model));
        return files;
    }

    static String overview(ModuleModel model) {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(model.systemName()).append("\n\n");
        out.append("Base package: `").append(model.basePackage()).append("`\n\n");
        out.append("```mermaid\n").append(Diagrams.mermaid(model)).append("```\n\n");
        out.append("| Module | Base package | Type | Depends on | Allowed dependencies |\n");
        out.append("|---|---|---|---|---|\n");
        for (ModuleDescriptor module : model.modules()) {
            out.append("| ").append(module.displayName())
                    .append(" | `").append(module.basePackage()).append('`')
                    .append(" | ").append(module.open() ? "open" : "closed")
                    .append(" | ").append(String.join(", ", module.dependencies()))
                    .append(" | ").append(module.allowedDependencies().map(it -> String.join(", ", it)).orElse("any"))
                    .append(" |\n");
        }
        if (!model.violations().isEmpty()) {
            out.append("\n## Violations\n\n");
            for (Violation violation : model.violations()) {
                out.append("1. ").append(violation.message().replace("\n", " ")).append('\n');
            }
        }
        return out.toString();
    }
}
