package io.quarkiverse.modulith.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Lets the test methods of a {@code @QuarkusTest} class declare {@link Scenario} parameters. Not needed on an
 * {@link ApplicationModuleTest}.
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ScenarioParameterResolver.class)
public @interface EnableScenarios {
}
