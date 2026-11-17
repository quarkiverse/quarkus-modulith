package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Compares the build time data as JSON text, so that the test compiles against Jackson 2 (Quarkus 3) and Jackson 3
 * (Quarkus 4).
 */
public class DevUiBuildTimeDataTest extends DevUIBuildTimeDataTest {

    static final String VALID = "io.quarkiverse.modulith.deployment.fixtures.valid";

    @RegisterExtension
    static final QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage())
                    .addAsResource(new StringAsset("quarkus.modulith.docs.enabled=false\n"), "application.properties"));

    public DevUiBuildTimeDataTest() {
        super("io.quarkiverse.modulith.quarkus-modulith");
    }

    @Test
    void exposesTheModulesRelationsViolationsAndListeners() throws Exception {
        // act
        List<String> keys = getAllKeys();

        // assert
        assertThat(keys).contains("modules", "relations", "violations", "listeners", "mermaid");
    }

    @Test
    void describesEachModule() throws Exception {
        // act
        List<String> modules = new ArrayList<>();
        getBuildTimeData("modules").forEach(it -> modules.add(it.toString()));

        // assert
        assertThat(modules).hasSize(5).filteredOn(it -> it.contains("\"name\":\"inventory\""))
                .containsExactly("{\"name\":\"inventory\",\"displayName\":\"Inventory\",\"basePackage\":\"" + VALID
                        + ".inventory\",\"type\":\"closed\",\"dependencies\":[\"order\"],\"allowedDependencies\":[\"order\"],"
                        + "\"namedInterfaces\":[],\"exposedTypes\":[\"" + VALID + ".inventory.InventoryManagement\"],"
                        + "\"publishedEvents\":[],\"listenedEvents\":[\"" + VALID
                        + ".order.OrderCompleted\"],\"classCount\":4}");
    }

    @Test
    void listsTheRelationsAndListenersWithoutViolations() throws Exception {
        // act
        String relations = getBuildTimeData("relations").toString();
        String violations = getBuildTimeData("violations").toString();
        String listeners = getBuildTimeData("listeners").toString();

        // assert
        assertThat(relations).isEqualTo("[{\"source\":\"catalog\",\"target\":\"shared\",\"type\":\"uses\",\"references\":1},"
                + "{\"source\":\"inventory\",\"target\":\"order\",\"type\":\"listens to\",\"references\":1},"
                + "{\"source\":\"shipping\",\"target\":\"catalog\",\"type\":\"uses\",\"references\":1},"
                + "{\"source\":\"shipping\",\"target\":\"order\",\"type\":\"uses\",\"references\":1}]");
        assertThat(violations).isEqualTo("[]");
        assertThat(listeners).isEqualTo("[{\"id\":\"" + VALID + ".inventory.internal.InventoryListener.on(" + VALID
                + ".order.OrderCompleted)\",\"module\":\"inventory\",\"eventType\":\"" + VALID
                + ".order.OrderCompleted\",\"async\":true}]");
    }
}
