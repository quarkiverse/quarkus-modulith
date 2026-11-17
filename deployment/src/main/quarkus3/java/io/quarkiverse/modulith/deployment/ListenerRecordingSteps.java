package io.quarkiverse.modulith.deployment;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.runtime.events.ListenerRecorder;
import io.quarkiverse.modulith.runtime.events.ListenerTable;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

/**
 * Exposes the listener table as a synthetic bean created by a bytecode recorder. Quarkus 3 variant, the Quarkus 4 one
 * uses the service registrar.
 */
class ListenerRecordingSteps {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void listenerTable(ListenerRecorder recorder, ModuleListenersBuildItem listeners,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ListenerTable.class)
                .scope(Singleton.class)
                .supplier(recorder.listenerTable(listeners.getListeners()))
                .done());
    }
}
