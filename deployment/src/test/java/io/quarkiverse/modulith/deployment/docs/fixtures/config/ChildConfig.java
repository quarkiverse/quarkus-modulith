package io.quarkiverse.modulith.deployment.docs.fixtures.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "shop.child")
public interface ChildConfig extends ParentConfig {

    String name();
}
