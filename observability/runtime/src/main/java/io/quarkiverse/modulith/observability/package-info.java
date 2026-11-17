/**
 * OpenTelemetry spans for calls into application modules and Micrometer counters for module events, with the names
 * and tags of Spring Modulith's observability support.
 * <p>
 * Customize spans with {@link io.quarkiverse.modulith.observability.ModulithSpanCustomizer} beans and event counters
 * with {@link io.quarkiverse.modulith.observability.ModulithEventMetricsCustomizer} beans.
 */
package io.quarkiverse.modulith.observability;
