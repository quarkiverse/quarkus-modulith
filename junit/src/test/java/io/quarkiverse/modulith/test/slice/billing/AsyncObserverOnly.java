package io.quarkiverse.modulith.test.slice.billing;

import jakarta.enterprise.event.ObservesAsync;

public class AsyncObserverOnly {

    void on(@ObservesAsync Long event) {
    }
}
