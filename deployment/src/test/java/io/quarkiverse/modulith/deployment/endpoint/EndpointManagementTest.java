package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EndpointManagementTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()).addClass(Http.class))
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.management.test-port", "0")
            .overrideConfigKey("quarkus.modulith.endpoint.path", "modules");

    @TestHTTPResource(value = "/modules", management = true)
    URL management;

    @TestHTTPResource("/q/modules")
    URL application;

    @Test
    void servesTheModuleModelOnTheManagementInterface() {
        // arrange
        URL url = management;

        // act
        HttpResponse<String> response = Http.get(url);

        // assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("{ \"order\" : { \"displayName\" : \"Order\"");
    }

    @Test
    void leavesTheApplicationInterfaceAlone() {
        // act
        HttpResponse<String> response = Http.get(application);

        // assert
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
