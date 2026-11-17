package io.quarkiverse.modulith.moments.runtime.jackson;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * Registers {@link MomentsJacksonModule} on the application's {@link ObjectMapper}. Registered by the
 * deployment module when {@code quarkus-jackson} is present.
 */
@Singleton
public class MomentsJacksonCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new MomentsJacksonModule());
    }
}
