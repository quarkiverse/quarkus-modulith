package io.quarkiverse.modulith.deployment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.deployment.model.InitializerOrderBuildItem;
import io.quarkiverse.modulith.runtime.initializer.ModuleInitializerInvoker;
import io.quarkiverse.modulith.runtime.model.ApplicationModulesRuntimeProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

/**
 * Support for {@link ApplicationModuleInitializer} beans: they run at startup in the dependency order of their modules,
 * computed here at build time.
 */
class InitializerProcessor {

    static final DotName INITIALIZER = DotName.createSimple(ApplicationModuleInitializer.class);
    static final DotName PRIORITY = DotName.createSimple("jakarta.annotation.Priority");

    @BuildStep
    UnremovableBeanBuildItem keepInitializers() {
        return UnremovableBeanBuildItem.beanTypes(ApplicationModuleInitializer.class);
    }

    /**
     * The invoker observes the startup event, which keeps it. The producer is unremovable because applications and
     * tests look its {@code ApplicationModules} and {@code ApplicationModulesRuntime} beans up programmatically too.
     */
    @BuildStep
    void runtimeBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(ModuleInitializerInvoker.class).build());
        beans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(ApplicationModulesRuntimeProducer.class)
                .setUnremovable().build());
    }

    @BuildStep
    InitializerOrderBuildItem initializerOrder(CombinedIndexBuildItem index, ModuleModelBuildItem model) {
        return new InitializerOrderBuildItem(order(index.getIndex(), model.getApplicationModules()));
    }

    /**
     * @return the concrete initializer classes, ordered by the dependency order of their modules, then by
     *         {@code @Priority}, then by name. Initializers outside any module come last.
     */
    static List<String> order(IndexView index, Optional<ApplicationModules> modules) {
        List<String> order = modules.map(it -> it.stream().map(ApplicationModule::getName).toList()).orElse(List.of());
        return index.getAllKnownImplementations(INITIALIZER).stream()
                .filter(it -> !it.isAbstract())
                .sorted(Comparator.<ClassInfo> comparingInt(it -> moduleIndex(it, modules, order))
                        .thenComparingInt(InitializerProcessor::priority)
                        .thenComparing(it -> it.name().toString()))
                .map(it -> it.name().toString())
                .toList();
    }

    private static int moduleIndex(ClassInfo type, Optional<ApplicationModules> modules, List<String> order) {
        return modules.flatMap(it -> it.getModuleForType(type.name().toString()))
                .map(it -> order.indexOf(it.getName()))
                .orElse(order.size());
    }

    private static int priority(ClassInfo type) {
        return Optional.ofNullable(type.declaredAnnotation(PRIORITY)).map(AnnotationInstance::value)
                .map(it -> it.asInt()).orElse(0);
    }
}
