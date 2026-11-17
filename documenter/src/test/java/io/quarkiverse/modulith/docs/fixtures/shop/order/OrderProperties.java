package io.quarkiverse.modulith.docs.fixtures.shop.order;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("shop.order")
public class OrderProperties {

    private int limit = 5;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
