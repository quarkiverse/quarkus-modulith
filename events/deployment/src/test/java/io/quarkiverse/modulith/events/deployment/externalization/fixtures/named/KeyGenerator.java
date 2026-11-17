package io.quarkiverse.modulith.events.deployment.externalization.fixtures.named;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("keys")
@ApplicationScoped
public class KeyGenerator {

    @Inject
    @Named("ignored")
    Thing injected;

    public String of(Object event) {
        return "key-" + event;
    }

    void consume(@Named("parameter") Thing thing) {
    }
}
