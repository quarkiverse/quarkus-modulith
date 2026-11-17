package io.quarkiverse.modulith.events.externalization.runtime;

/**
 * A drain whose run ends with an error, such as a class that fails to load.
 */
class BrokenDrain extends OutboxDrain {

    @Override
    void runOnce() {
        throw new LinkageError("broken class");
    }
}
