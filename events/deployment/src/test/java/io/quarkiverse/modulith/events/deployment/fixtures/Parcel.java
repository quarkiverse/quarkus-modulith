package io.quarkiverse.modulith.events.deployment.fixtures;

import java.util.List;

public record Parcel(String id, Address address, List<String> items) implements Shipment {
}
