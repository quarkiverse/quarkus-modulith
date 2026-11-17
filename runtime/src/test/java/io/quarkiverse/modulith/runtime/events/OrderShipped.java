package io.quarkiverse.modulith.runtime.events;

record OrderShipped(String id) implements Shipped, Tracked {
}
