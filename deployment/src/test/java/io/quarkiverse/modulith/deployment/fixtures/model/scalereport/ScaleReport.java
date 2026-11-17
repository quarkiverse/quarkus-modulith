package io.quarkiverse.modulith.deployment.fixtures.model.scalereport;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkus.runtime.StartupEvent;

/**
 * Logs answers of the recorded model at startup, so a production mode test can assert them from the console output.
 */
@ApplicationScoped
public class ScaleReport {

    private static final Logger LOG = Logger.getLogger(ScaleReport.class);

    private static final String ROOT = "io.quarkiverse.modulith.deployment.fixtures.model.scale";

    void report(@Observes StartupEvent event, ApplicationModules modules) {
        long start = System.nanoTime();
        ApplicationModule last = modules.getModuleByName("m19").orElseThrow();
        String module = modules.getModuleForType(ROOT + ".m05.internal.Impl7").map(ApplicationModule::getName).orElse("");
        String direct = last.getDirectDependencies(modules).uniqueModules().stream()
                .map(ApplicationModule::getName).sorted()
                .collect(Collectors.joining(","));
        long dependencies = modules.stream()
                .mapToLong(it -> it.getDirectDependencies(modules).stream().count())
                .sum();
        String internal = last.getInternalTypes().stream().map(JavaType::getName).sorted().findFirst().orElse("");
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        LOG.infof("scale modules=%d module=%s direct=%s dependencies=%d interfaces=%s internal=%s queries=%dms",
                modules.stream().count(), module, direct, dependencies, last.getNamedInterfaces(), internal, elapsed);
    }
}
