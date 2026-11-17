package io.quarkiverse.modulith.flyway.deployment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import io.quarkus.flyway.FlywayConfigurationCustomizer;

/**
 * Adds an application callback, which module migrations keep.
 */
@Singleton
public class RecordingCustomizer implements FlywayConfigurationCustomizer {

    final List<String> tables = new CopyOnWriteArrayList<>();

    @Override
    public void customize(FluentConfiguration configuration) {
        configuration.callbacks(Stream.concat(Stream.of(configuration.getCallbacks()), Stream.of(new TableRecorder(tables)))
                .toArray(Callback[]::new));
    }
}
