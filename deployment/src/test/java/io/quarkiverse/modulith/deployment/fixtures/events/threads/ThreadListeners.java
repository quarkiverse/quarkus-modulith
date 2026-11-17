package io.quarkiverse.modulith.deployment.fixtures.events.threads;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkus.runtime.BlockingOperationControl;

/**
 * Records on which kind of thread each listener ran.
 */
public class ThreadListeners {

    public static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(async = false, id = "sync")
    void sync(Ping event) {
        record("sync", event);
    }

    @ApplicationModuleListener(id = "async")
    void async(Ping event) {
        record("async", event);
    }

    static void record(String listener, Ping event) {
        Thread thread = Thread.currentThread();
        CALLS.add(listener + ":" + event.id() + ":virtual=" + thread.isVirtual() + ":blocking="
                + BlockingOperationControl.isBlockingAllowed());
    }
}
