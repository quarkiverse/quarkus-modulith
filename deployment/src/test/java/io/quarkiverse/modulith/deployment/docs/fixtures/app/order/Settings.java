package io.quarkiverse.modulith.deployment.docs.fixtures.app.order;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Settings {

    @ConfigProperty(name = "shop.flag", defaultValue = "true")
    boolean flag;

    @ConfigProperty
    int unnamed;

    @ConfigProperty(name = "")
    String blank;

    @ConfigProperty(name = "shop.explicit", defaultValue = ConfigProperty.UNCONFIGURED_VALUE)
    String explicit;

    final Duration timeout;

    @Inject
    Settings(@ConfigProperty(name = "shop.timeout") Duration timeout) {
        this.timeout = timeout;
    }

    Settings() {
        this(Duration.ZERO);
    }
}
