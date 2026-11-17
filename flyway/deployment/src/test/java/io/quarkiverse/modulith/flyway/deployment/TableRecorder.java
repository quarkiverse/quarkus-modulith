package io.quarkiverse.modulith.flyway.deployment;

import java.util.List;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

public final class TableRecorder implements Callback {

    private final List<String> tables;

    public TableRecorder(List<String> tables) {
        this.tables = tables;
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        tables.add(context.getConfiguration().getTable());
    }

    @Override
    public String getCallbackName() {
        return "table recorder";
    }
}
