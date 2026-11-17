package io.quarkiverse.modulith.deployment.fixtures.events.requestcontext;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds a value per request, to tell the request context of the publisher from a fresh one.
 */
@RequestScoped
public class RequestMarker {

    public static final String FRESH = "fresh";

    private String mark = FRESH;

    public String mark() {
        return mark;
    }

    public void mark(String value) {
        mark = value;
    }
}
