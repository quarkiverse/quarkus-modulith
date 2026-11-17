package io.quarkiverse.modulith.deployment.docs;

import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The documentation model of the application, for the documentation, the {@code /q/modulith} endpoint and the Dev UI.
 */
public final class DocumentationModelBuildItem extends SimpleBuildItem {

    private final DocumentationModel model;

    public DocumentationModelBuildItem(DocumentationModel model) {
        this.model = model;
    }

    public DocumentationModel getModel() {
        return model;
    }
}
