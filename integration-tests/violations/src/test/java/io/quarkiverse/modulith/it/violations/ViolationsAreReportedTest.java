package io.quarkiverse.modulith.it.violations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ApplicationModulesRuntime;
import io.quarkiverse.modulith.core.Violations;
import io.quarkiverse.modulith.core.util.ApplicationModulesExporter;
import io.quarkiverse.modulith.it.violations.customer.Customers;
import io.quarkus.test.junit.QuarkusTest;

/**
 * With {@code quarkus.modulith.verification.mode=warn} the application starts, and the violations can be checked in a
 * test, as with Spring Modulith's {@code ApplicationModules.verify()}.
 */
@QuarkusTest
class ViolationsAreReportedTest {

    @Inject
    ApplicationModules modules;

    @Inject
    ApplicationModulesRuntime runtime;

    @Test
    void reportsTheInternalAccess() {
        // act
        Violations violations = modules.detectViolations();

        // assert
        assertThat(violations.getViolations()).containsExactlyInAnyOrderElementsOf(Expected.VIOLATIONS);
    }

    @Test
    void failsEveryVerificationOfTheSharedBean() {
        // arrange
        Violations violations = modules.detectViolations();

        // act & assert
        assertThatThrownBy(modules::verify).isSameAs(violations);
        assertThatThrownBy(modules::verify).isSameAs(violations);
    }

    @Test
    void exposesTheRuntimeModel() {
        // act
        boolean applicationClass = runtime.isApplicationClass(Customers.class);

        // assert
        assertThat(applicationClass).isTrue();
        assertThat(runtime.get()).isSameAs(modules);
        assertThat(runtime.getRuntime().getMainApplicationClass()).hasValue(ViolationsApplication.class);
        assertThat(runtime.getRuntime().getId()).hasValue("quarkus-modulith-integration-tests-violations");
    }

    @Test
    void exportsTheModelInSpringModulithsFormat() throws IOException {
        // arrange
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // act
        String json;
        try (InputStream in = Objects.requireNonNull(loader.getResourceAsStream(ApplicationModulesExporter.DEFAULT_LOCATION),
                "export not generated at " + ApplicationModulesExporter.DEFAULT_LOCATION)) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // assert
        assertThat(json).isEqualTo("{ \"customer\" : { \"displayName\" : \"Customer\", \"basePackage\" : \"" + Expected.BASE
                + ".customer\", \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"namedInterfaces\" : {"
                + " \"<<UNNAMED>>\" : [ \"" + Expected.BASE + ".customer.CustomerInitializer\", \"" + Expected.BASE
                + ".customer.Customers\" ] }, \"initializers\" : [ \"" + Expected.BASE + ".customer.CustomerInitializer\" ],"
                + " \"dependencies\" : [] }, \"billing\" : { \"displayName\" : \"Billing\", \"basePackage\" : \""
                + Expected.BASE + ".billing\", \"nested\" : [], \"type\" : \"closed\", \"shared\" : false,"
                + " \"namedInterfaces\" : { \"<<UNNAMED>>\" : [ \"" + Expected.BASE + ".billing.BillingInitializer\", \""
                + Expected.SERVICE + "\" ] }, \"initializers\" : [ \"" + Expected.BASE + ".billing.BillingInitializer\" ],"
                + " \"allowedDependencies\" : [ \"customer\" ], \"dependencies\" : [ { \"target\" : \"customer\","
                + " \"types\" : [ \"USES_COMPONENT\" ] } ] } }");
    }
}
