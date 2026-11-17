package io.quarkiverse.modulith.core.par.beans.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.quarkiverse.modulith.core.par.beans.notify.Notifier;

@Configuration
public class AppConfig {
    @Bean
    Clock clock(Notifier notifier) {
        return new Clock();
    }
}
