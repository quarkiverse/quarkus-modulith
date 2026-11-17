package io.quarkiverse.modulith.runtime.events;

import java.time.Duration;

record FakeListenerConfig(Duration shutdownTimeout, CompletionTiming completionTiming) implements ListenerConfig {
}
