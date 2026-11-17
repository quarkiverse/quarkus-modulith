package io.quarkiverse.modulith.core.par.advanced.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Customer {
    @Id
    Long id;
}
