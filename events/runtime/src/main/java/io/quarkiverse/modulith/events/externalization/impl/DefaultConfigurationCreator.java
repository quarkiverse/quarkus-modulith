package io.quarkiverse.modulith.events.externalization.impl;

import java.util.List;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the default {@link EventExternalizationConfiguration} when the application declares none: the
 * {@code Externalized} types below the application's base packages, routed by the annotation or their
 * application-local name. Without base package, every {@code Externalized} type, routed by the annotation or its fully
 * qualified name. Like Spring Modulith's {@code EventExternalizationAutoConfiguration}.
 */
public class DefaultConfigurationCreator implements BeanCreator<EventExternalizationConfiguration> {

    /**
     * The parameter that carries the base packages.
     */
    public static final String PACKAGES = "packages";

    @Override
    public EventExternalizationConfiguration create(SyntheticCreationalContext<EventExternalizationConfiguration> context) {
        List<String> packages = List.of((String[]) context.getParams().get(PACKAGES));
        ExternalizationRuntimeConfig config = context.getInjectedReference(ExternalizationRuntimeConfig.class);
        EventExternalizationConfiguration.Router router = packages.isEmpty()
                ? EventExternalizationConfiguration.externalizing().annotatedAsExternalized()
                : EventExternalizationConfiguration.defaults(packages);
        return router.serializeExternalization(config.serializeExternalization()).build();
    }
}
