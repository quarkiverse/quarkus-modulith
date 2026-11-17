package io.quarkiverse.modulith.deployment.fixtures.model.edge.alpha;

import java.util.List;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.deployment.fixtures.model.edge.beta.api.First;
import io.quarkiverse.modulith.deployment.fixtures.model.edge.beta.extra.Extra1;
import io.quarkiverse.modulith.deployment.fixtures.model.edge.beta.extra.Extra2;
import io.quarkiverse.modulith.deployment.fixtures.model.edge.open.internal.Util;

/**
 * Exercises the reference and event detection corners of the model builder.
 */
public class Alpha<T> {

    Event<?> wildcard;
    Event<List<String>> generic;
    Event<T> typeVariable;
    Event<AlphaEvent> published;
    String notAnEvent;

    Object references(Object input) {
        First first = new First();
        Extra1 one = new Extra1();
        Extra2 two = new Extra2();
        Util util = new Util();
        Object matrix = new String[1][1];
        int[] numbers = (int[]) input;
        return List.of(first, one, two, util, matrix, numbers);
    }

    void observe(@Observes String text, @ObservesAsync AlphaEvent event, AlphaEvent plain) {
    }

    @ApplicationModuleListener
    void withoutParameters() {
    }

    public record AlphaEvent(String id) {
    }
}
