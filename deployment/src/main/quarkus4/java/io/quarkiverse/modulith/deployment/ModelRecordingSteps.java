package io.quarkiverse.modulith.deployment;

import java.util.List;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.core.ApplicationRuntime;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.deployment.model.InitializerOrderBuildItem;
import io.quarkiverse.modulith.runtime.initializer.InitializerOrder;
import io.quarkiverse.modulith.runtime.model.QuarkusApplicationRuntime;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

/**
 * Publishes the application runtime and the initializer order as static-init services backing their beans. The module
 * model itself is a resource the runtime reads on first use. Quarkus 4 variant, the Quarkus 3 one uses a bytecode recorder.
 */
class ModelRecordingSteps {

    @BuildStep
    void applicationModulesBean(ServiceRegistrar services, ModuleModelBuildItem model, ApplicationInfoBuildItem application,
            InitializerOrderBuildItem initializers, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        ModelData data = model.getModelData();
        String id = application.getName();
        String mainClass = ModelProcessor.mainClass(model).orElse("");
        List<String> rootPackages = List.copyOf(data.rootPackages());
        services.forService(ApplicationRuntime.class)
                .atPhase(Phase.STATIC_INIT)
                .onStart(context -> new QuarkusApplicationRuntime(id, mainClass, rootPackages));
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ApplicationRuntime.class)
                .scope(Singleton.class)
                .unremovable()
                .serviceValue(ApplicationRuntime.class)
                .done());

        List<String> order = initializers.getClassNames();
        services.forService(InitializerOrder.class)
                .atPhase(Phase.STATIC_INIT)
                .onStart(context -> new InitializerOrder(order));
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(InitializerOrder.class)
                .scope(Singleton.class)
                .serviceValue(InitializerOrder.class)
                .done());
    }
}
