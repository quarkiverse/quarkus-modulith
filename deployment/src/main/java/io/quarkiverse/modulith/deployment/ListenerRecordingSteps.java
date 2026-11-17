package io.quarkiverse.modulith.deployment;

import java.util.List;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.runtime.events.ListenerData;
import io.quarkiverse.modulith.runtime.events.ListenerTable;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Builds the listener table in a static-init service backing the {@link ListenerTable} bean.
 */
class ListenerRecordingSteps {

    @BuildStep
    void listenerTable(ServiceRegistrar services, ModuleListenersBuildItem listeners,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        List<ListenerData> table = List.copyOf(listeners.getListeners());
        services.forService(ListenerTable.class)
                .atPhase(Phase.STATIC_INIT)
                .onStart(context -> new ListenerTable(table));
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ListenerTable.class)
                .scope(Singleton.class)
                .serviceValue(ListenerTable.class)
                .done());
    }
}
