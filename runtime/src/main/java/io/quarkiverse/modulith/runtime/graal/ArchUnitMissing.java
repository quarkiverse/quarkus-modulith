package io.quarkiverse.modulith.runtime.graal;

import java.util.function.BooleanSupplier;

/**
 * Holds when ArchUnit, an optional dependency of the core, is not part of the native image.
 */
public final class ArchUnitMissing implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return MissingClass.isMissing("com.tngtech.archunit.lang.ArchRule", ArchUnitMissing.class.getClassLoader());
    }
}
