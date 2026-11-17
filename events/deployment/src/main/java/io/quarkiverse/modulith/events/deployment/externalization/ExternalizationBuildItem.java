package io.quarkiverse.modulith.events.deployment.externalization;

import io.quarkiverse.modulith.events.ExternalizationMode;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Whether event externalization is active in this application, and in which mode. Externalization is active when it
 * is enabled and a transport exists.
 */
public final class ExternalizationBuildItem extends SimpleBuildItem {

    private final boolean active;
    private final ExternalizationMode mode;

    public ExternalizationBuildItem(boolean active, ExternalizationMode mode) {
        this.active = active;
        this.mode = mode;
    }

    public boolean isActive() {
        return active;
    }

    public ExternalizationMode getMode() {
        return mode;
    }
}
