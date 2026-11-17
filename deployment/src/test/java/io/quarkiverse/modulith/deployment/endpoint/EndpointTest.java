package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EndpointTest {

    static final String VALID = "io.quarkiverse.modulith.deployment.fixtures.valid.";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()).addClass(Http.class));

    @TestHTTPResource("/q/modulith")
    URL endpoint;

    @Test
    void servesTheModuleModel() {
        // arrange
        URL url = endpoint;

        // act
        HttpResponse<String> response = Http.get(url);

        // assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(response.body()).isEqualTo("{ "
                + "\"order\" : { \"displayName\" : \"Order\", \"basePackage\" : \"" + VALID + "order\", "
                + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"dependencies\" : [] }, "
                + "\"shared\" : { \"displayName\" : \"Shared\", \"basePackage\" : \"" + VALID + "shared\", "
                + "\"nested\" : [], \"type\" : \"open\", \"shared\" : true, \"dependencies\" : [] }, "
                + "\"catalog\" : { \"displayName\" : \"Catalog\", \"basePackage\" : \"" + VALID + "catalog\", "
                + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"dependencies\" : [ { \"target\" : "
                + "\"shared\", \"types\" : [ \"DEFAULT\" ] } ] }, "
                + "\"inventory\" : { \"displayName\" : \"Inventory\", \"basePackage\" : \"" + VALID + "inventory\", "
                + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"allowedDependencies\" : [ \"order\" ], "
                + "\"dependencies\" : [ { \"target\" : \"order\", \"types\" : [ \"EVENT_LISTENER\" ] } ] }, "
                + "\"shipping\" : { \"displayName\" : \"Shipping\", \"basePackage\" : \"" + VALID + "shipping\", "
                + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"allowedDependencies\" : "
                + "[ \"catalog :: spi\", \"order\" ], \"dependencies\" : [ { \"target\" : \"catalog\", \"types\" : "
                + "[ \"USES_COMPONENT\" ] }, { \"target\" : \"order\", \"types\" : [ \"DEFAULT\" ] } ] } }");
    }
}
