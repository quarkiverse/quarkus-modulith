package io.quarkiverse.modulith.observability.runtime;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link ObservedModules} bean from the rows the build passes as the {@code rows} parameter.
 */
public class ObservedModulesCreator implements BeanCreator<ObservedModules> {

    /**
     * The name of the synthetic bean parameter holding the rows.
     */
    public static final String ROWS = "rows";

    @Override
    public ObservedModules create(SyntheticCreationalContext<ObservedModules> context) {
        return ObservedModules.of((String[]) context.getParams().get(ROWS));
    }
}
