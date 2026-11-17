package org.springframework.modulith.events.jackson;

import org.springframework.modulith.events.core.EventSerializer;

import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Modulith's Jackson serializer with a default mapper, as Spring Boot configures it without customization.
 */
public final class SpringSerializers {

    private SpringSerializers() {
    }

    public static EventSerializer jackson() {
        return new JacksonEventSerializer(() -> JsonMapper.builder().build());
    }
}
