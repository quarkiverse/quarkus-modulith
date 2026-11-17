package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named("keys")
@ApplicationScoped
public class Keys {

    public String of(Object event) {
        return "k-" + event.getClass().getSimpleName();
    }
}
