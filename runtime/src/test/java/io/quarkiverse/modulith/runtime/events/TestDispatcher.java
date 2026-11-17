package io.quarkiverse.modulith.runtime.events;

import java.util.List;

/**
 * The dispatcher with the request context and the thread kind under the test's control.
 */
final class TestDispatcher extends ModuleEventDispatcher {

    final List<String> contextCalls;
    boolean contextActive;
    boolean blockingAllowed = true;

    TestDispatcher(List<String> contextCalls) {
        this.contextCalls = contextCalls;
    }

    @Override
    boolean activateRequestContext() {
        contextCalls.add(contextActive ? "already-active" : "activate");
        return !contextActive;
    }

    @Override
    void terminateRequestContext() {
        contextCalls.add("terminate");
    }

    @Override
    boolean isBlockingAllowed() {
        return blockingAllowed;
    }
}
