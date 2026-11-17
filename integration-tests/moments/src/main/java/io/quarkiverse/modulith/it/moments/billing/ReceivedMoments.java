package io.quarkiverse.modulith.it.moments.billing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The moments the billing module received, as text.
 */
@ApplicationScoped
public class ReceivedMoments {

    private final List<String> received = new CopyOnWriteArrayList<>();

    void add(Object moment) {
        received.add(moment.toString());
    }

    List<String> sorted() {
        return received.stream().sorted().toList();
    }

    void clear() {
        received.clear();
    }
}
