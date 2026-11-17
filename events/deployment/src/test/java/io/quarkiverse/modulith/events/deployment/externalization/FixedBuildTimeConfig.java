package io.quarkiverse.modulith.events.deployment.externalization;

import io.quarkiverse.modulith.events.ExternalizationMode;

record FixedBuildTimeConfig(boolean enabled, ExternalizationMode mode) implements ExternalizationBuildTimeConfig {
}
