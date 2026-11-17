package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EndpointRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()).addClass(Http.class))
            .overrideConfigKey("quarkus.http.non-application-root-path", "internal")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @TestHTTPResource("/internal/modulith")
    URL moved;

    @TestHTTPResource("/q/modulith")
    URL defaultLocation;

    @Test
    void servesTheModuleModelBelowTheConfiguredRoot() {
        // act
        HttpResponse<String> response = Http.get(moved);

        // assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("{ \"order\" : { \"displayName\" : \"Order\"");
    }

    @Test
    void leavesTheDefaultRootFree() {
        // act
        HttpResponse<String> response = Http.get(defaultLocation);

        // assert
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
