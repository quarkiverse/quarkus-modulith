package io.quarkiverse.modulith.it.violations;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RuntimeResourceTest {

    @Test
    void answersFromTheRuntimeModel() {
        // act
        String body = given().get("/runtime").then().statusCode(200).extract().asString();

        // assert
        assertThat(body).isEqualTo(String.join("\n",
                "initializers: customer, billing",
                "modules: customer, billing",
                "module of InvoiceService: billing",
                "billing depends on: customer",
                "customer internals: " + Expected.REPOSITORY,
                "main class: " + ViolationsApplication.class.getName()));
    }
}
