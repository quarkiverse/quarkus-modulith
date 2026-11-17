package io.quarkiverse.modulith.deployment.fixtures.cycle.a;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.deployment.fixtures.cycle.b.B;

@Singleton
public class A {

    B b;
}
