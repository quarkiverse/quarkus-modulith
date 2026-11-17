package io.quarkiverse.modulith.core;

import java.util.function.Supplier;

/**
 * The module model of the running application together with the {@link ApplicationRuntime}, like Spring Modulith's
 * {@code ApplicationModulesRuntime}. Quarkus Modulith provides it as a bean.
 */
public final class ApplicationModulesRuntime implements Supplier<ApplicationModules> {

    private final ApplicationModules modules;
    private final ApplicationRuntime runtime;

    public ApplicationModulesRuntime(ApplicationModules modules, ApplicationRuntime runtime) {
        this.modules = modules;
        this.runtime = runtime;
    }

    @Override
    public ApplicationModules get() {
        return modules;
    }

    /**
     * @return whether the type belongs to the application or to one of its modules
     */
    public boolean isApplicationClass(Class<?> type) {
        return runtime.isApplicationClass(type) || modules.contains(type);
    }

    /**
     * @return the type the bean was declared with
     */
    public Class<?> getUserClass(Object bean, String beanName) {
        return runtime.getUserClass(bean, beanName);
    }

    public ApplicationRuntime getRuntime() {
        return runtime;
    }
}
