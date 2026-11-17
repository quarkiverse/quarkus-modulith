package io.quarkiverse.modulith.observability.runtime;

/**
 * A bean class of a module.
 */
class Target {

    public String call(String[] values) {
        return "called";
    }

    public String other() {
        return "other";
    }
}
