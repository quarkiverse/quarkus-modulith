package io.quarkiverse.modulith.deployment.model;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.model.devmode.DevApplication;
import io.quarkiverse.modulith.deployment.fixtures.model.devmode.Endpoint;
import io.quarkiverse.modulith.deployment.fixtures.model.devmode.billing.Billing;
import io.quarkiverse.modulith.deployment.fixtures.model.devmode.billing.CustomerListener;
import io.quarkiverse.modulith.deployment.fixtures.model.devmode.customer.Customer;
import io.quarkiverse.modulith.deployment.fixtures.model.devmode.customer.internal.Repository;
import io.quarkus.test.QuarkusDevModeTest;

public class DevModeReloadTest {

    static final String BASE = "io.quarkiverse.modulith.deployment.fixtures.model.devmode";

    @RegisterExtension
    static final QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(DevApplication.class, Endpoint.class, Billing.class, Customer.class,
                    Repository.class)
                    .addAsResource(new StringAsset("quarkus.modulith.docs.enabled=false\nquarkus.http.port=0\n"),
                            "application.properties"));

    @Test
    void reportsAViolationRecoversAndPicksUpListeners() {
        // arrange
        String healthy = get("/listened").then().statusCode(200).extract().asString();

        // act
        devMode.modifySourceFile(Billing.class, it -> it.replace("// reference",
                BASE + ".customer.internal.Repository repository;"));
        String broken = get("/listened").then().statusCode(500).extract().asString();
        devMode.modifySourceFile(Billing.class, it -> it.replace(BASE + ".customer.internal.Repository repository;",
                "// reference"));
        String fixed = get("/listened").then().statusCode(200).extract().asString();
        devMode.addSourceFile(CustomerListener.class);
        String listened = get("/listened").then().statusCode(200).extract().asString();

        // assert
        assertThat(healthy).isEqualTo("[]");
        assertThat(broken).contains("Module 'billing' depends on non-exposed type " + BASE
                + ".customer.internal.Repository within module 'customer'!");
        assertThat(fixed).isEqualTo("[]");
        assertThat(listened).isEqualTo("[" + BASE + ".customer.Customer]");
    }
}
