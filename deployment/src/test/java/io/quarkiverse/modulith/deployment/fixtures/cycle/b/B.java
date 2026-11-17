package io.quarkiverse.modulith.deployment.fixtures.cycle.b;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.deployment.fixtures.cycle.a.A;

@Singleton
public class B {

    A a;
}
