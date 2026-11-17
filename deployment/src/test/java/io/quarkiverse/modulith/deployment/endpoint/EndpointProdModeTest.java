package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;

public class EndpointProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest prod = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .setApplicationName("endpoint")
            .setApplicationVersion("1.0")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-vertx-http", Version.getVersion())))
            .setRuntimeProperties(Map.of("quarkus.http.port", "0"))
            .setRun(true);

    @Test
    void doesNotServeTheModuleModelInProductionByDefault() throws MalformedURLException {
        // arrange
        URI uri = URI.create("http://localhost:" + port(prod.getStartupConsoleOutput()) + "/q/modulith");

        // act
        HttpResponse<String> response = Http.get(uri.toURL());

        // assert
        assertThat(response.statusCode()).isEqualTo(404);
    }

    private static String port(String output) {
        Matcher matcher = Pattern.compile("Listening on: http://[^:]+:([0-9]+)").matcher(output);
        assertThat(matcher.find()).as("the HTTP port in the startup log").isTrue();
        return matcher.group(1);
    }
}
