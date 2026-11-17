package io.quarkiverse.modulith.runtime.endpoint;

import java.util.ArrayList;
import java.util.List;

import io.vertx.ext.web.RoutingContext;

/**
 * Serves {@code /q/modulith}: the module model as JSON, computed at build time in the format of Spring Modulith's
 * {@code /actuator/modulith} endpoint.
 */
public final class ModulithEndpointHandler implements ModulithEndpoint {

    /**
     * The longest part, in chars: at most three bytes each in a class file constant, which holds 65535 bytes.
     */
    static final int PART_LENGTH = 16_384;

    private final String json;

    private ModulithEndpointHandler(String json) {
        this.json = json;
    }

    /**
     * @param parts the module model, see {@code ApplicationModulesJson#simple}, split by {@link #parts(String)}
     */
    public static ModulithEndpointHandler of(List<String> parts) {
        return new ModulithEndpointHandler(String.join("", parts));
    }

    /**
     * Splits the JSON so that the build can record it whatever the size of the model: each part fits a class file
     * string constant.
     */
    public static List<String> parts(String json) {
        List<String> parts = new ArrayList<>();
        for (int start = 0; start < json.length(); start += PART_LENGTH) {
            parts.add(json.substring(start, Math.min(json.length(), start + PART_LENGTH)));
        }
        return parts;
    }

    @Override
    public void handle(RoutingContext context) {
        context.response().putHeader("Content-Type", "application/json").end(json);
    }
}
