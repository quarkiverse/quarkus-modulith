package io.quarkiverse.modulith.deployment.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class DocsCustomizedProdModeTest {

    static final String OUTPUT = "custom/docs";

    @RegisterExtension
    static final QuarkusProdModeTest prod = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addPackages(true, DocsFixture.BASE))
            .setApplicationName("docs-customized")
            .setApplicationVersion("1.0")
            .overrideConfigKey("quarkus.modulith.docs.output-directory", OUTPUT)
            .overrideConfigKey("quarkus.modulith.docs.diagram.style", "uml")
            .overrideConfigKey("quarkus.modulith.docs.diagram.dependency-depth", "all")
            .overrideConfigKey("quarkus.modulith.docs.diagram.dependency-types", "event-listener")
            .overrideConfigKey("quarkus.modulith.docs.diagram.elements-without-relationships", "visible")
            .overrideConfigKey("quarkus.modulith.docs.canvas.groupings", "spring")
            .overrideConfigKey("quarkus.modulith.docs.canvas.reveal-internals", "true")
            .overrideConfigKey("quarkus.modulith.docs.canvas.reveal-empty-lines", "true")
            .overrideConfigKey("quarkus.modulith.docs.canvas.api-base", "https://docs.example/api");

    @ProdBuildResults
    ProdModeTestResults results;

    @Test
    void writesWithTheConfiguredOptions() throws IOException {
        // arrange
        Path output = results.getBuildDir().resolve(OUTPUT);

        // act
        String diagram = Files.readString(output.resolve("components.puml"));
        String canvas = Files.readString(output.resolve("module-order.adoc"));

        // assert
        assertThat(diagram).startsWith("@startuml\n\nset separator none\ntop to bottom direction\nhide stereotype\n")
                .contains("component \"==Catalog\\n<size:16>[Component: Module]</size>\"",
                        "Docs.Docs.Inventory --> Docs.Docs.Orders <<Relationship-UmVsYXRpb25zaGlw>> : \"listens to\"")
                .doesNotContain(": \"uses\"");
        assertThat(canvas).contains("|Description\n|\n", "_Configuration properties_",
                "link:https://docs.example/api/io/quarkiverse/modulith/deployment/docs/fixtures/app/order/OrderService.html[`i.q.m.d.d.f.a.o.OrderService`]",
                "`i.q.m.d.d.f.a.o.internal.OrderStore`", "|Aggregate roots\n|* \n");
        assertThat(results.getBuildDir().resolve("modulith-docs")).doesNotExist();
    }
}
