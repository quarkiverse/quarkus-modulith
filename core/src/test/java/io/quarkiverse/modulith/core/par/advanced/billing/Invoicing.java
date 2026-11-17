package io.quarkiverse.modulith.core.par.advanced.billing;

import jakarta.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.quarkiverse.modulith.core.par.advanced.customer.CustomerRepository;
import io.quarkiverse.modulith.core.par.advanced.order.OrderPlaced;

@Component
public class Invoicing {
    @Inject
    CustomerRepository customers;

    @EventListener
    void on(OrderPlaced event) {
    }
}
