package io.quarkiverse.modulith.it.common.internal;

import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static String next(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
