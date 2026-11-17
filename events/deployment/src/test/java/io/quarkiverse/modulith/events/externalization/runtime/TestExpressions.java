package io.quarkiverse.modulith.events.externalization.runtime;

import io.quarkiverse.modulith.events.externalization.fakes.routing.Shape;
import io.quarkiverse.modulith.events.externalization.fakes.routing.Square;
import io.quarkiverse.modulith.events.externalization.impl.CompiledRoutingExpressions;

class TestExpressions extends CompiledRoutingExpressions {

    TestExpressions() {
        register(Shape.class.getName(), "{event.id}", event -> ((Square) event).id());
        register(Shape.class.getName(), "{event.topic}", event -> "shapes");
        register(Shape.class.getName(), "{event.blank}", event -> " ");
        register(Shape.class.getName(), "{event.none}", event -> null);
    }
}
