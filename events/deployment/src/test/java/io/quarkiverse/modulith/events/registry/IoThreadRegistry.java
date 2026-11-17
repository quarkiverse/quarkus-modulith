package io.quarkiverse.modulith.events.registry;

/**
 * A registry that believes it runs on a Vert.x I/O thread.
 */
final class IoThreadRegistry extends DefaultEventPublicationRegistry {

    @Override
    boolean isBlockingAllowed() {
        return false;
    }
}
