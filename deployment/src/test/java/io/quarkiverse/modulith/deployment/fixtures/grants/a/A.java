package io.quarkiverse.modulith.deployment.fixtures.grants.a;

import io.quarkiverse.modulith.deployment.fixtures.grants.b.B;
import io.quarkiverse.modulith.deployment.fixtures.grants.b.api.Api;

public class A {

    Api api;

    Object base() {
        return new B();
    }
}
