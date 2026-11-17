package io.quarkiverse.modulith.core.par.beans.notify;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import io.quarkiverse.modulith.core.par.beans.config.Clock;

@Component
public class Notifier implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
    }

    @TransactionalEventListener
    void on(Clock clock) {
    }
}
