package io.quarkiverse.modulith.events.externalization.fakes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

public class FakeTransport implements EventExternalizationTransport {

    private final List<List<Object>> sent = new CopyOnWriteArrayList<>();
    private final Supplier<CompletionStage<?>> answer;

    public FakeTransport(Supplier<CompletionStage<?>> answer) {
        this.answer = answer;
    }

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        sent.add(List.of(payload, target, headers));
        return answer.get();
    }

    public List<List<Object>> sent() {
        return List.copyOf(sent);
    }
}
