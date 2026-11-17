package io.quarkiverse.modulith;

/**
 * A bean that initializes its application module at startup. Initializers run in the dependency order of their
 * modules, so an initializer can rely on the modules its module depends on being initialized, like Spring Modulith's
 * {@code ApplicationModuleInitializer}.
 * <p>
 * Implementations are CDI beans in a module package. Beans implementing this interface are kept even if nothing
 * injects them, and run at startup. Within a module, {@code @Priority} (lower first) and then the class name decide.
 */
@FunctionalInterface
public interface ApplicationModuleInitializer {

    /**
     * Initializes the module. Exceptions abort the startup.
     */
    void initialize();
}
