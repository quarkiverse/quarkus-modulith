package io.quarkiverse.modulith.it.violations;

import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ApplicationRuntime;
import io.quarkiverse.modulith.core.JavaType;

/**
 * Answers from the runtime model and the initializer log, so the tests can check them in the JVM and natively.
 */
@Path("/runtime")
public class RuntimeResource {

    @Inject
    ApplicationModules modules;

    @Inject
    ApplicationRuntime runtime;

    @Inject
    Initializations initializations;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String describe() {
        ApplicationModule billing = modules.getModuleByName("billing").orElseThrow();
        ApplicationModule customer = modules.getModuleByName("customer").orElseThrow();
        return String.join("\n",
                "initializers: " + String.join(", ", initializations.modules()),
                "modules: " + modules.stream().map(ApplicationModule::getName).collect(Collectors.joining(", ")),
                "module of InvoiceService: " + modules.getModuleForType(getClass().getPackageName()
                        + ".billing.InvoiceService").map(ApplicationModule::getName).orElse("none"),
                "billing depends on: " + billing.getDirectDependencies(modules).uniqueModules().stream()
                        .map(ApplicationModule::getName).sorted().collect(Collectors.joining(", ")),
                "customer internals: " + customer.getInternalTypes().stream().map(JavaType::getName).sorted()
                        .collect(Collectors.joining(", ")),
                "main class: " + runtime.getMainApplicationClass().map(Class::getName).orElse("none"));
    }
}
