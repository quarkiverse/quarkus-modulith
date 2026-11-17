package io.quarkiverse.modulith.runtime;

/**
 * The capabilities the Modulith extensions provide and check, shared so that providers and consumers agree on the
 * names.
 */
public final class ModulithCapabilities {

    /**
     * Provided by every event publication store, which then has an {@code EventPublicationRepository} bean.
     */
    public static final String EVENT_PUBLICATION_REPOSITORY = "io.quarkiverse.modulith.events.repository";

    private ModulithCapabilities() {
    }
}
