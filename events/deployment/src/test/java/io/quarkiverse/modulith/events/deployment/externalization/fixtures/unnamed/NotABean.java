package io.quarkiverse.modulith.events.deployment.externalization.fixtures.unnamed;

import jakarta.inject.Named;

@Named("loose")
public class NotABean {

    public String of(Object event) {
        return "loose";
    }
}
