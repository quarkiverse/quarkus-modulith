package io.quarkiverse.modulith.it.externalizationrabbitmq.audit;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.modulith.events.EventExternalized;

@ApplicationScoped
public class ExternalizedRecords {

    private final List<ExternalizedRecord> records = new CopyOnWriteArrayList<>();

    void on(@Observes EventExternalized<?, ?> event) {
        Object result = event.getBrokerResult();
        records.add(new ExternalizedRecord(event.getTarget().toString(),
                Objects.isNull(result) ? "none" : result.getClass().getSimpleName()));
    }

    public List<ExternalizedRecord> records() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
