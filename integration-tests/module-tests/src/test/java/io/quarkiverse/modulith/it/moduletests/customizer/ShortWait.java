package io.quarkiverse.modulith.it.moduletests.customizer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.awaitility.core.ConditionFactory;

import io.quarkiverse.modulith.test.ScenarioCustomizer;
import io.quarkus.arc.ArcContainer;

/**
 * Waits 150 milliseconds by default and records which methods it customized.
 */
public class ShortWait implements ScenarioCustomizer {

    static final List<String> CUSTOMIZED = new CopyOnWriteArrayList<>();

    @Override
    public Function<ConditionFactory, ConditionFactory> getDefaultCustomizer(Method method, ArcContainer container) {
        CUSTOMIZED.add(method.getName() + ":" + container.isRunning());
        return it -> it.atMost(Duration.ofMillis(150));
    }
}
