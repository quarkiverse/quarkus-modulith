package io.quarkiverse.modulith.it.externalizationjms.order;

import java.util.List;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders")
public record OrderPlaced(String id, Customer customer, List<OrderLine> lines) {
}
