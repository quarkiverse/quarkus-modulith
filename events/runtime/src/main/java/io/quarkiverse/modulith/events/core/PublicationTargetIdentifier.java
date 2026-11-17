package io.quarkiverse.modulith.events.core;

/**
 * Identifies the listener a publication is meant for. Stored as {@code listener_id}.
 *
 * @param value the listener identifier, never blank
 */
public record PublicationTargetIdentifier(String value) {

    public PublicationTargetIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Publication target identifier must not be blank");
        }
    }

    public static PublicationTargetIdentifier of(String value) {
        return new PublicationTargetIdentifier(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
