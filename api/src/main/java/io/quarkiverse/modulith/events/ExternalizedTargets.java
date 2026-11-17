package io.quarkiverse.modulith.events;

import java.util.Optional;

/**
 * Caches the routing target declared on each event type. A {@link ClassValue}, so cached entries never keep a class
 * loader alive.
 */
final class ExternalizedTargets extends ClassValue<Optional<ParsedRoutingTarget>> {

    @Override
    protected Optional<ParsedRoutingTarget> computeValue(Class<?> type) {
        return AnnotationTargetLookup.lookup(type);
    }
}
