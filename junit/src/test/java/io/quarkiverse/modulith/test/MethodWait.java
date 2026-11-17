package io.quarkiverse.modulith.test;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.awaitility.core.ConditionFactory;

import io.quarkus.arc.ArcContainer;

/**
 * A customizer registered on a test method that always returns the same function.
 */
public class MethodWait implements ScenarioCustomizer {

    static final Function<ConditionFactory, ConditionFactory> CUSTOMIZER = it -> it;

    @Override
    public Function<ConditionFactory, ConditionFactory> getDefaultCustomizer(Method method, ArcContainer container) {
        return CUSTOMIZER;
    }
}
