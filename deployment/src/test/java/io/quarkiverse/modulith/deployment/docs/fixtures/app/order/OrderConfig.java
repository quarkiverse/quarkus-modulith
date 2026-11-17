package io.quarkiverse.modulith.deployment.docs.fixtures.app.order;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "shop.order")
public interface OrderConfig {

    @WithDefault("5")
    int maxSize();

    @WithName("region-code")
    String region();

    Optional<String> note();

    List<String> tags();

    Limits limits();

    Optional<Limits> backup();

    @WithParentName
    Extra extra();

    default int twice() {
        return maxSize() * 2;
    }

    interface Limits {

        int daily();
    }

    interface Extra {

        String mode();
    }
}
