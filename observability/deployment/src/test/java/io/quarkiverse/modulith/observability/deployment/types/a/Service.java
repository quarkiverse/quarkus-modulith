package io.quarkiverse.modulith.observability.deployment.types.a;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Produces;

public class Service extends Base implements Comparable<Service> {

    static final List<String> NAMES = List.of(String.valueOf(System.nanoTime()));

    public Service() {
    }

    @Override
    public void overridden() {
    }

    void parameters(String[][] matrix, List<? extends Number> numbers,
            Map<String, io.quarkiverse.modulith.observability.deployment.types.b.Event> events) {
    }

    <T> void generic(T value) {
        Runnable task = () -> hidden();
        task.run();
    }

    private void hidden() {
    }

    static void util() {
    }

    @Produces
    String produced() {
        return "produced";
    }

    @Override
    public int compareTo(Service other) {
        return 0;
    }
}
