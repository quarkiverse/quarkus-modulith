package io.quarkiverse.modulith.it.externalizationjms;

import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.util.List;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

final class Orders {

    static final Duration TIMEOUT = Duration.ofSeconds(20);

    private Orders() {
    }

    static String json(String id) {
        return "{\"id\":\"" + id + "\",\"customer\":{\"name\":\"Ada\",\"email\":\"ada@example.com\"},"
                + "\"lines\":[{\"sku\":\"s-1\",\"quantity\":2},{\"sku\":\"s-2\",\"quantity\":1}]}";
    }

    static void place(String id) {
        given().contentType(ContentType.JSON).body(json(id)).post("/orders").then().statusCode(202);
    }

    static JsonPath receive(String id) {
        return given().queryParam("timeout", TIMEOUT.toMillis()).get("/messages/orders/" + id).then().statusCode(200)
                .extract().jsonPath();
    }

    static int receiveStatus(String id, long timeoutMillis) {
        return given().queryParam("timeout", timeoutMillis).get("/messages/orders/" + id).then().extract()
                .statusCode();
    }

    static List<String> statuses(String id) {
        return given().get("/publications").then().statusCode(200).extract().jsonPath()
                .getList("findAll { it.event.startsWith('OrderPlaced[id=" + id + ",') }.status", String.class);
    }

    static List<String> brokerResults(String id) {
        return given().get("/publications/externalized").then().statusCode(200).extract().jsonPath()
                .getList("findAll { it.target == 'orders' }.brokerResult", String.class);
    }
}
