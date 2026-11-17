package io.quarkiverse.modulith.it.observability;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
class ObservabilityTest {

    static final String PLACE = "Orders > i.q.m.i.o.o.OrderService.place(j.l.String)";
    static final String EVENTS = "module_events_published_total{event_type=\"order.OrderPlaced\","
            + "module_identifier=\"order\",module_name=\"Orders\"}";
    static final String ORDER_PLACED = "module_events_published_order_OrderPlaced_total{module_identifier=\"order\","
            + "module_name=\"Orders\"}";
    static final String INVOCATIONS = "module_invocations_seconds_count{error=\"none\",module_identifier=\"order\","
            + "module_invocation_type=\"bean\",module_name=\"Orders\"}";
    static final String FAILED = "modulith_event_publications{status=\"failed\"}";
    static final String RESOURCE = "Orders > i.q.m.i.o.o.OrderResource.place(j.l.String)";
    static final String LISTENER = "Inventory > i.q.m.i.o.i.InventoryListener.on(i.q.m.i.o.o.OrderPlaced)";

    @BeforeEach
    void clear() {
        given().delete("/telemetry/spans").then().statusCode(204);
    }

    @Test
    void tracesCallsIntoModules() {
        // arrange
        String id = "42";

        // act
        given().post("/orders/" + id).then().statusCode(200);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> given().get("/telemetry/spans").then()
                .body("name", hasItem(RESOURCE))
                .body("name", hasItem(LISTENER))
                .body("name", not(hasItem(PLACE)))
                .body("find { it.name == '" + RESOURCE + "' }.attributes", equalTo(Map.of(
                        "module.identifier", "order", "module.name", "Orders", "module.invocation-type", "bean",
                        "module.method", "place")))
                .body("find { it.name == '" + LISTENER + "' }.attributes.'module.invocation-type'",
                        equalTo("event-listener")));
    }

    @Test
    void countsModuleEvents() {
        // arrange
        String id = "43";
        double events = metric(EVENTS);
        double orderPlaced = metric(ORDER_PLACED);
        double invocations = metric(INVOCATIONS);

        // act
        given().post("/orders/" + id).then().statusCode(200);

        // assert
        assertThat(metric(EVENTS), equalTo(events + 1));
        assertThat(metric(ORDER_PLACED), equalTo(orderPlaced + 1));
        assertThat(metric(INVOCATIONS), equalTo(invocations + 1));
    }

    @Test
    void countsPublicationsOfTheStore() {
        // arrange
        String lost = "lost";
        double failed = metric(FAILED);

        // act
        given().post("/orders/" + lost).then().statusCode(200);

        // assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(metric(FAILED), equalTo(failed + 1)));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> given().get("/q/metrics").then().statusCode(200)
                .body(containsString("modulith_event_publications{status=\"published\"} 0.0"))
                .body(not(containsString("modulith_event_publications{status=\"completed\"}"))));
    }

    @Test
    void observesRestDataResources() {
        // arrange
        String product = "{\"name\":\"tea\"}";

        // act
        given().contentType("application/json").body(product).post("/product").then().statusCode(201);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> given().get("/telemetry/spans").then()
                .body("name", hasItem(containsString("Catalog > i.q.m.i.o.c.ProductResource.add("))));
    }

    @Test
    void servesTheModuleModel() {
        // arrange
        String path = "/q/modulith";

        // act
        Response response = given().get(path);

        // assert
        response.then().statusCode(200).body("order.displayName", equalTo("Orders"))
                .body("inventory.dependencies.target", hasItem("order"));
    }

    /**
     * @return the value of the series in the Prometheus scrape, without the exemplar that may follow it, {@code 0}
     *         while the series does not exist
     */
    static double metric(String series) {
        return given().get("/q/metrics").then().statusCode(200).extract().asString().lines()
                .filter(it -> it.startsWith(series + " "))
                .mapToDouble(it -> Double.parseDouble(it.substring(series.length() + 1).split(" ")[0]))
                .findFirst()
                .orElse(0);
    }
}
