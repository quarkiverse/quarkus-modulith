package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Named("tags")
@Dependent
public class Tags {

    public static final AtomicInteger DESTROYED = new AtomicInteger();

    public String tag(Tagged event) {
        return "tag-" + event.label().name();
    }

    public String refuse(Untagged event) {
        throw new IllegalArgumentException("no tag for " + event.name());
    }

    @PreDestroy
    void destroy() {
        DESTROYED.incrementAndGet();
    }
}
