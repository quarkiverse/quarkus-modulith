package io.quarkiverse.modulith.docs.fixtures.config.billing;

import io.smallrye.config.ConfigMapping;

/**
 * A mapping without prefix: its properties are top-level names.
 */
@ConfigMapping
public interface Flags {

    boolean dryRun();
}
