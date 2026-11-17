package io.quarkiverse.modulith.docs.fixtures.shop.order;

import org.springframework.stereotype.Controller;

@Controller
public class OrderController {

    private final OrderManagement orders;

    OrderController(OrderManagement orders) {
        this.orders = orders;
    }

    OrderManagement orders() {
        return orders;
    }
}
