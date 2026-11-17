package io.quarkiverse.modulith.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

/**
 * A {@code @QuarkusTest} that starts one application module, and optionally its dependencies, instead of the whole
 * application. The Quarkus counterpart of Spring Modulith's {@code @ApplicationModuleTest}.
 * <p>
 * Quarkus builds one application per test profile, so the test also names an {@link ApplicationModuleTestProfile}
 * subclass, which takes its settings from this annotation:
 *
 * <pre>
 * package com.acme.inventory;
 *
 * &#64;ApplicationModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
 * &#64;TestProfile(InventoryModuleTest.Profile.class)
 * class InventoryModuleTest {
 *
 *     public static class Profile extends ApplicationModuleTestProfile {
 *     }
 *
 *     &#64;Test
 *     void reservesStock(Scenario scenario) {
 *         // …
 *     }
 * }
 * </pre>
 *
 * Test methods may declare {@link Scenario}, {@link PublishedEvents}, {@link AssertablePublishedEvents} and
 * {@link ModuleTestExecution} parameters.
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@QuarkusTest
@ExtendWith({ ApplicationModuleTestExtension.class, PublishedEventsExtension.class, ScenarioParameterResolver.class })
public @interface ApplicationModuleTest {

    /**
     * Alias for {@link #mode()}.
     */
    BootstrapMode value() default BootstrapMode.STANDALONE;

    /**
     * Which other modules start besides the module under test. When both this and {@link #value()} are set, the one
     * that is not {@link BootstrapMode#STANDALONE} wins.
     */
    BootstrapMode mode() default BootstrapMode.STANDALONE;

    /**
     * Whether the build of the test application fails on any module rule violation, also when
     * {@code quarkus.modulith.verification.mode} is {@code warn}.
     */
    boolean verifyAutomatically() default true;

    /**
     * Identifiers of modules to start in addition to those of the bootstrap mode.
     */
    String[] extraIncludes() default {};

    /**
     * The module under test, as identifier or package, when the test class is not in the module's package.
     */
    String module() default "";
}
