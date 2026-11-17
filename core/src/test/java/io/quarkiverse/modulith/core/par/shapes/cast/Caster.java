package io.quarkiverse.modulith.core.par.shapes.cast;

import io.quarkiverse.modulith.core.par.shapes.target.internal.Thing;

public class Caster {

    public Object cast(Object value) {
        return (Thing) value;
    }
}
