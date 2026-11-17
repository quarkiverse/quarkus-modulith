package io.quarkiverse.modulith.runtime.events;

import java.util.List;

import io.quarkiverse.modulith.events.spi.ReadOnlyTransactions;

record FakeReadOnly(List<String> calls) implements ReadOnlyTransactions {

    @Override
    public void markReadOnly() {
        calls.add("read-only");
    }
}
