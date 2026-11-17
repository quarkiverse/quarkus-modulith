package io.quarkiverse.modulith.core.par.advanced.customer;

import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {
    public Customer find(long id) {
        return new Customer();
    }
}
