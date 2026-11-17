package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * Nested modules carry their parent, and the enclosing module lists them, as in Spring Modulith's actuator JSON.
 */
public class EndpointNestedModulesTest {

    static final String NESTED = "io.quarkiverse.modulith.deployment.fixtures.nested";
    static final String PACKAGE = NESTED + ".";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, NESTED).addClass(Http.class))
            .overrideConfigKey("quarkus.modulith.base-package", NESTED);

    @TestHTTPResource("/q/modulith")
    URL endpoint;

    @Test
    void servesTheParentAndTheNestedModules() {
        // arrange
        URL url = endpoint;

        // act
        HttpResponse<String> response = Http.get(url);

        // assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{ "
                + "\"billing\" : { \"displayName\" : \"Billing\", \"basePackage\" : \"" + PACKAGE + "billing\", "
                + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"dependencies\" : [] }, "
                + "\"order\" : { \"displayName\" : \"Order\", \"basePackage\" : \"" + PACKAGE + "order\", "
                + "\"nested\" : [ \"order.other\", \"order.sub\" ], \"type\" : \"closed\", \"shared\" : false, "
                + "\"dependencies\" : [] }, "
                + "\"order.other\" : { \"displayName\" : \"Other\", \"basePackage\" : \"" + PACKAGE + "order.other\", "
                + "\"parent\" : \"order\", \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, "
                + "\"dependencies\" : [] }, "
                + "\"order.sub\" : { \"displayName\" : \"Sub\", \"basePackage\" : \"" + PACKAGE + "order.sub\", "
                + "\"parent\" : \"order\", \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, "
                + "\"dependencies\" : [] } }");
    }
}
