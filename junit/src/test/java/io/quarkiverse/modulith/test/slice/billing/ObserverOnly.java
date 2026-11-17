package io.quarkiverse.modulith.test.slice.billing;

import jakarta.enterprise.event.Observes;

public class ObserverOnly {

    void on(@Observes Integer event) {
    }
}
