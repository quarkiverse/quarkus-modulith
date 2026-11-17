package io.quarkiverse.modulith.moments.runtime.jackson;

import jakarta.inject.Singleton;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers {@link MomentsJacksonModule} on the application's {@link JsonMapper}. Quarkus 4 variant, registered by
 * the deployment module when {@code quarkus-jackson} is present.
 */
@Singleton
public class MomentsJacksonCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.addModule(new MomentsJacksonModule());
    }
}
