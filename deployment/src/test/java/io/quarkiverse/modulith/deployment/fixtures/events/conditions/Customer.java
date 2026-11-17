package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

public class Customer {

    private final String name;
    private final boolean vip;

    public Customer(String name, boolean vip) {
        this.name = name;
        this.vip = vip;
    }

    public String getName() {
        return name;
    }

    public boolean isVip() {
        return vip;
    }
}
