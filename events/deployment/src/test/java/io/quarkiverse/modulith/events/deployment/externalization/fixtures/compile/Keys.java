package io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named("keys")
@ApplicationScoped
public class Keys {

    public String of(Object event) {
        return "of";
    }

    public String forEvent(CompileEvent event) {
        return "event";
    }

    public String forLabel(Labeled event) {
        return "label";
    }

    public String count(int count) {
        return "count";
    }

    public String text(String text) {
        return text;
    }

    public Object pick(Object event) {
        return event;
    }

    public Labeled pick(CompileEvent event) {
        return event;
    }

    public String either(Labeled first, Object second) {
        return "labeled-first";
    }

    public String either(Object first, Labeled second) {
        return "labeled-second";
    }
}
