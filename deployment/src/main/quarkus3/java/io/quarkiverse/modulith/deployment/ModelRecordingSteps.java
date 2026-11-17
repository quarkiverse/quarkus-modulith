package io.quarkiverse.modulith.deployment;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.core.ApplicationRuntime;
import io.quarkiverse.modulith.deployment.model.InitializerOrderBuildItem;
import io.quarkiverse.modulith.runtime.initializer.InitializerOrder;
import io.quarkiverse.modulith.runtime.model.ModelRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

/**
 * Records the application runtime and the initializer order with a bytecode recorder. The module model itself is a
 * resource the runtime reads on first use. Quarkus 3 variant, the Quarkus 4 one uses the service registrar.
 */
class ModelRecordingSteps {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void applicationModulesBean(ModelRecorder recorder, ModuleModelBuildItem model, ApplicationInfoBuildItem application,
            InitializerOrderBuildItem initializers, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ApplicationRuntime.class)
                .scope(Singleton.class)
                .unremovable()
                .supplier(recorder.applicationRuntime(application.getName(), ModelProcessor.mainClass(model).orElse(""),
                        model.getModelData().rootPackages()))
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(InitializerOrder.class)
                .scope(Singleton.class)
                .supplier(recorder.initializerOrder(initializers.getClassNames()))
                .done());
    }
}
