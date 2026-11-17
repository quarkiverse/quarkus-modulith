package io.quarkiverse.modulith.runtime.initializer;

import java.util.Comparator;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkus.runtime.StartupEvent;

/**
 * Runs the {@link ApplicationModuleInitializer} beans at startup, in module dependency order.
 */
@Singleton
public class ModuleInitializerInvoker {

    private static final Logger LOG = Logger.getLogger(ModuleInitializerInvoker.class);

    @Inject
    @Any
    Instance<ApplicationModuleInitializer> initializers;

    @Inject
    InitializerOrder order;

    void onStart(@Observes StartupEvent event) {
        initializers.handlesStream()
                .sorted(Comparator.comparingInt(it -> order.indexOf(it.getBean().getBeanClass())))
                .forEach(ModuleInitializerInvoker::invoke);
    }

    /**
     * Runs the initializer of the handle, then destroys a {@code @Dependent} instance, which nothing else holds, also
     * when the initializer fails.
     */
    static void invoke(Instance.Handle<ApplicationModuleInitializer> handle) {
        String name = handle.getBean().getBeanClass().getName();
        try {
            LOG.debugf("Initializing %s.", name);
            handle.get().initialize();
            LOG.debugf("Initializing %s done.", name);
        } finally {
            if (handle.getBean().getScope() == Dependent.class) {
                handle.destroy();
            }
        }
    }
}
