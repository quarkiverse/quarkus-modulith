package io.quarkiverse.modulith.events.deployment.externalization;

import io.quarkiverse.modulith.runtime.events.ListenerBuildTimeConfig;
import io.quarkiverse.modulith.runtime.events.ListenerExecutorKind;

record FixedListenerConfig(ListenerExecutorKind executor) implements ListenerBuildTimeConfig {
}
