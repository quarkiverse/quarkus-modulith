package io.quarkiverse.modulith.core.par.propagate.core.internal;

import java.util.List;

@org.springframework.modulith.NamedInterface("facade")
public class Facade {
    public Result run(Input input) {
        return new Result();
    }

    public List<Item> items() {
        return List.of();
    }

    Secret secret() {
        return null;
    }
}
