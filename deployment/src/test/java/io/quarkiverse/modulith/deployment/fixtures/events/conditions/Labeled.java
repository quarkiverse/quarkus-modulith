package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

/**
 * Reaches {@link Base} twice, so the accessor search meets an interface it has seen already.
 */
public interface Labeled extends Coded, Named {
}
