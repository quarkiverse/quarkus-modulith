package io.quarkiverse.modulith.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The application module model, available to other extensions at build time. Everything it exposes is immutable.
 */
public final class ModuleModelBuildItem extends SimpleBuildItem {

    private final ModuleModel model;
    private final String mermaid;

    /**
     * @param model the model, {@code null} when the application has no base package
     * @param mermaid the module diagram in Mermaid syntax
     */
    public ModuleModelBuildItem(ModuleModel model, String mermaid) {
        this.model = model;
        this.mermaid = mermaid;
    }

    /**
     * @return the model, empty when the application has no base package
     */
    public Optional<ModuleModel> model() {
        return Optional.ofNullable(model);
    }

    /**
     * Kept for the extensions that read the model as a nullable value, prefer {@link #model()}.
     *
     * @return the model, {@code null} when the application has no base package
     */
    public ModuleModel getModel() {
        return model;
    }

    public String getMermaid() {
        return mermaid;
    }

    /**
     * @return the core model, empty when the application has no base package
     */
    public Optional<ApplicationModules> getApplicationModules() {
        return model().map(ModuleModel::applicationModules);
    }

    /**
     * @return the model in the form recorded for runtime use
     */
    public ModelData getModelData() {
        return getApplicationModules().map(ApplicationModules::getModelData)
                .orElseGet(() -> new ModelData("", "", List.of(), List.of(), mermaid));
    }
}
