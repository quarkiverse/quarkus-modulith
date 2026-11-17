package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModulesRuntime;
import io.quarkiverse.modulith.core.ApplicationRuntime;
import io.quarkiverse.modulith.core.util.ApplicationModulesExporter;
import io.quarkiverse.modulith.deployment.fixtures.model.init.Calls;
import io.quarkiverse.modulith.deployment.fixtures.model.init.base.BaseInitializer;
import io.quarkus.test.QuarkusExtensionTest;

public class InitializersTest {

    static final String INIT = "io.quarkiverse.modulith.deployment.fixtures.model.init";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, INIT))
            .overrideConfigKey("quarkus.modulith.base-package", INIT)
            .overrideConfigKey("quarkus.log.category.\"io.quarkiverse.modulith.runtime.initializer\".level", "DEBUG")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.export.enabled", "true");

    @Inject
    ApplicationModulesRuntime runtime;

    @Inject
    ApplicationRuntime application;

    @Inject
    BaseInitializer base;

    @Test
    void runsTheInitializersInModuleOrder() {
        // assert
        assertThat(Calls.CALLS).containsExactly("base", "early top", "late top", "late top destroyed", "top", "root");
    }

    @Test
    void describesTheRuntime() {
        // act
        Class<?> userClass = runtime.getUserClass(base, "base");

        // assert
        assertThat(userClass).isEqualTo(BaseInitializer.class);
        assertThat(application.getMainApplicationClass()).isEmpty();
        assertThat(application.getId()).hasValue("quarkus-modulith-deployment");
        assertThat(runtime.isApplicationClass(BaseInitializer.class)).isTrue();
        assertThat(runtime.isApplicationClass(String.class)).isFalse();
    }

    @Test
    void exportsTheModelInSpringModulithsFormat() throws IOException {
        // arrange
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // act
        String json;
        try (InputStream in = loader.getResourceAsStream(ApplicationModulesExporter.DEFAULT_LOCATION)) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // assert
        assertThat(json).isEqualTo("{ \"base\" : { \"displayName\" : \"Base\", \"basePackage\" : \"" + INIT + ".base\","
                + " \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"namedInterfaces\" : { \"<<UNNAMED>>\" : [ \""
                + INIT + ".base.BaseInitializer\" ] }, \"initializers\" : [ \"" + INIT + ".base.BaseInitializer\" ],"
                + " \"dependencies\" : [] }, \"top\" : { \"displayName\" : \"Top\", \"basePackage\" : \"" + INIT + ".top\","
                + " \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"namedInterfaces\" : { \"<<UNNAMED>>\" : [ \""
                + INIT + ".top.AbstractInitializer\", \"" + INIT + ".top.EarlyTopInitializer\", \"" + INIT
                + ".top.LateTopInitializer\", \"" + INIT + ".top.MarkerInitializer\", \"" + INIT + ".top.TopInitializer\" ] },"
                + " \"initializers\" : [ \"" + INIT + ".top.EarlyTopInitializer\", \"" + INIT + ".top.LateTopInitializer\", \""
                + INIT
                + ".top.TopInitializer\" ], \"dependencies\" : [ { \"target\" : \"base\", \"types\" : [ \"DEFAULT\" ] } ] } }");
    }
}
