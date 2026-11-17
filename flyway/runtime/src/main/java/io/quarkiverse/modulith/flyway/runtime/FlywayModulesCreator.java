package io.quarkiverse.modulith.flyway.runtime;

import java.util.List;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the synthetic {@link FlywayModules} bean from the module list the build passes as the {@code modules}
 * parameter.
 */
public class FlywayModulesCreator implements BeanCreator<FlywayModules> {

    public static final String MODULES = "modules";

    @Override
    public FlywayModules create(SyntheticCreationalContext<FlywayModules> context) {
        return new FlywayModules(List.of((String[]) context.getParams().get(MODULES)));
    }
}
