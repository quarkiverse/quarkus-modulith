package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

/**
 * A position in the publications ordered by publication date, then identifier. Pages continue after it.
 *
 * @param publicationDate the publication date of the last publication read
 * @param identifier the identifier of the last publication read
 */
public record PublicationCursor(Instant publicationDate, UUID identifier) {

    /**
     * The order the default implementations page in. Stores that page in the database use the database's order of
     * the same two columns, consistently.
     */
    public static final Comparator<TargetEventPublication> ORDER = Comparator
            .comparing(TargetEventPublication::getPublicationDate)
            .thenComparing(TargetEventPublication::getIdentifier);

    /**
     * @return the cursor right after the publication
     */
    public static PublicationCursor of(TargetEventPublication publication) {
        return new PublicationCursor(publication.getPublicationDate(), publication.getIdentifier());
    }

    /**
     * @return whether the publication comes after this cursor in {@link #ORDER}
     */
    public boolean precedes(TargetEventPublication publication) {
        int byDate = publication.getPublicationDate().compareTo(publicationDate);
        return byDate > 0 || byDate == 0 && publication.getIdentifier().compareTo(identifier) > 0;
    }
}
