package io.quarkiverse.modulith.events.jobrunr.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;

/**
 * Gives JobRunr a Jackson 3 JSON mapper. JobRunr's Quarkus extension 9.0.4 picks its Jackson 2 mapper whenever the
 * Jackson capability is present, but Quarkus 4 only brings Jackson 3, so JobRunr fails at startup with a
 * {@code NoClassDefFoundError}. The build excludes that Jackson 2 producer, and this reserve bean takes its place, so
 * that a {@link JsonMapper} bean of the application still replaces it.
 */
@Singleton
public class Jackson3JsonMapperProducer {

    @Produces
    @Singleton
    @Reserve
    @Priority(1)
    JsonMapper jobRunrJsonMapper() {
        return new Jackson3JsonMapper();
    }
}
