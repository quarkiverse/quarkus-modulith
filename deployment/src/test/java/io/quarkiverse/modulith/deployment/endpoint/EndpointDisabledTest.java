package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EndpointDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()).addClass(Http.class))
            .overrideConfigKey("quarkus.modulith.endpoint.enabled", "false");

    @TestHTTPResource("/q/modulith")
    URL endpoint;

    @Test
    void doesNotServeTheModuleModel() {
        // arrange
        URL url = endpoint;

        // act
        HttpResponse<String> response = Http.get(url);

        // assert
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
