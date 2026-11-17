package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Which page of publications to read: published strictly before an instant, after a cursor, at most a number of
 * items, ordered by publication date, then identifier.
 *
 * @param publishedBefore only publications published strictly before this instant
 * @param after only publications after this position, empty for the first page
 * @param maxItems at most this many publications, at least 1
 */
public record PageCriteria(Instant publishedBefore, Optional<PublicationCursor> after, int maxItems) {

    public PageCriteria {
        Objects.requireNonNull(publishedBefore, "publishedBefore");
        Objects.requireNonNull(after, "after");
        if (maxItems < 1) {
            throw new IllegalArgumentException("maxItems must be at least 1: " + maxItems);
        }
    }

    /**
     * @return the first page of publications published before the instant
     */
    public static PageCriteria first(Instant publishedBefore, int maxItems) {
        return new PageCriteria(publishedBefore, Optional.empty(), maxItems);
    }

    /**
     * @return the page after the given publication, with the same bounds
     */
    public PageCriteria next(TargetEventPublication last) {
        return new PageCriteria(publishedBefore, Optional.of(PublicationCursor.of(last)), maxItems);
    }

    /**
     * @return whether the publication belongs to this page's range, ignoring the size
     */
    public boolean matches(TargetEventPublication publication) {
        return publication.getPublicationDate().isBefore(publishedBefore)
                && after.map(cursor -> cursor.precedes(publication)).orElse(true);
    }
}
