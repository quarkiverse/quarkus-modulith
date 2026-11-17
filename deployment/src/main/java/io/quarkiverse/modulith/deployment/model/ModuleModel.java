package io.quarkiverse.modulith.deployment.model;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.VerificationOptions;
import io.quarkiverse.modulith.core.Violation;

/**
 * The application modules as other build steps see them: an immutable view of the core {@link ApplicationModules}.
 */
public final class ModuleModel {

    /**
     * The module rules only: the additional rules the configuration selects already ran in {@code ModelProcessor} and
     * their violations are recorded in the model.
     */
    private static final VerificationOptions RECORDED = VerificationOptions.defaults().withoutAdditionalVerifications();

    private final ApplicationModules modules;
    private final List<ModuleDescriptor> descriptors;
    private final boolean verified;

    /**
     * @param modules the core model
     * @param verified whether the verification ran, otherwise {@link #violations()} is empty
     */
    public ModuleModel(ApplicationModules modules, boolean verified) {
        this.modules = modules;
        this.verified = verified;
        this.descriptors = modules.stream().map(it -> new ModuleDescriptor(it, modules))
                .sorted(Comparator.comparing(ModuleDescriptor::name))
                .toList();
    }

    /**
     * @return the core model
     */
    public ApplicationModules applicationModules() {
        return modules;
    }

    /**
     * @return the declared system name, or the last segment of the base package
     */
    public String systemName() {
        String declared = modules.getSystemName();
        String base = modules.getBasePackage();
        return declared.isEmpty() ? base.substring(base.lastIndexOf('.') + 1) : declared;
    }

    public String basePackage() {
        return modules.getBasePackage();
    }

    public List<ModuleDescriptor> modules() {
        return descriptors;
    }

    public List<String> sharedModules() {
        return modules.getModelData().sharedModules();
    }

    /**
     * @return the violations the build recorded, those of the module rules and of the configured additional rules,
     *         empty when the verification is disabled. The rules are not evaluated again
     */
    public List<Violation> violations() {
        return verified ? modules.detectViolations(RECORDED).getViolations() : List.of();
    }

    public Optional<ModuleDescriptor> module(String name) {
        return descriptors.stream().filter(m -> m.name().equals(name)).findFirst();
    }

    /**
     * @return the module containing the type, if any
     */
    public Optional<ModuleDescriptor> moduleOf(String className) {
        return modules.getModuleForType(className).flatMap(it -> module(it.getName()));
    }

    /**
     * Finds a module by identifier, or by a package or type name inside it.
     */
    public Optional<ModuleDescriptor> resolve(String nameOrPackage) {
        return module(nameOrPackage)
                .or(() -> modules.getModuleForPackage(nameOrPackage).flatMap(it -> module(it.getName())))
                .or(() -> moduleOf(nameOrPackage));
    }
}
