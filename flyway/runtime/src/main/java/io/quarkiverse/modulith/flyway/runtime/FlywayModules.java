package io.quarkiverse.modulith.flyway.runtime;

import java.util.List;

/**
 * The modules whose migrations run, in dependency order (dependencies first), computed at build time. Modules left
 * out of a module test are not in the list.
 *
 * @param identifiers the module identifiers
 */
public record FlywayModules(List<String> identifiers) {

    public FlywayModules {
        identifiers = List.copyOf(identifiers);
    }
}
