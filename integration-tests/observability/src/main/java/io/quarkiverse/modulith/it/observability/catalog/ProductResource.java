package io.quarkiverse.modulith.it.observability.catalog;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;

public interface ProductResource extends PanacheEntityResource<Product, Long> {
}
