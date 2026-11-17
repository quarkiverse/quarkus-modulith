package io.quarkiverse.modulith.core.par.advanced.order;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.quarkiverse.modulith.core.par.advanced.customer.Customer;
import io.quarkiverse.modulith.core.par.advanced.customer.CustomerRepository;
import io.quarkiverse.modulith.core.par.advanced.customer.internal.Scoring;

@Service
public class OrderManagement {
    private final CustomerRepository customers;
    private final ApplicationEventPublisher events;

    public OrderManagement(CustomerRepository customers, ApplicationEventPublisher events) {
        this.customers = customers;
        this.events = events;
    }

    public Customer complete(long id) {
        events.publishEvent(new OrderPlaced(id));
        new Scoring().score();
        return customers.find(id);
    }
}
