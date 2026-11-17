package io.quarkiverse.modulith.it.externalizationjobrunr;

import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.util.Map;

import io.restassured.http.ContentType;

final class Shop {

    static final Duration TIMEOUT = Duration.ofSeconds(60);

    private Shop() {
    }

    static String json(String id) {
        return "{\"id\":\"" + id + "\",\"customer\":{\"name\":\"Ada\",\"email\":\"ada@example.com\"},"
                + "\"lines\":[{\"sku\":\"s-1\",\"quantity\":2},{\"sku\":\"s-2\",\"quantity\":1}]}";
    }

    static void place(String id) {
        given().contentType(ContentType.JSON).body(json(id)).post("/orders").then().statusCode(202);
    }

    static String deliveries(String transport) {
        return given().get("/deliveries/" + transport).then().statusCode(200).extract().asString();
    }

    static String delivered(String target, String id) {
        return "[{\"target\":\"" + target + "\",\"event\":" + json(id) + "}]";
    }

    static int attempts(String transport) {
        return Integer.parseInt(given().get("/deliveries/" + transport + "/attempts").then().statusCode(200)
                .extract().asString());
    }

    static void failNext(String transport, int times) {
        given().post("/deliveries/" + transport + "/failures/" + times).then().statusCode(204);
    }

    static Map<String, Integer> jobs() {
        return given().get("/deliveries/jobs").then().statusCode(200).extract().jsonPath().getMap("", String.class,
                Integer.class);
    }

    static String externalized() {
        return given().get("/deliveries/externalized").then().statusCode(200).extract().asString();
    }
}
