package io.quarkiverse.modulith.moments.runtime.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson 2 module that lets an {@code ObjectMapper} write and read the passage of time events and
 * {@code ShiftedQuarter}. Quarkus 3 variant. The extension registers it on the application's mapper, register it
 * yourself on a mapper you create.
 */
public class MomentsJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the module with the mix-ins of all moment types.
     */
    public MomentsJacksonModule() {
        super("quarkus-modulith-moments");
        MomentsMixins.MIXINS.forEach(this::setMixInAnnotation);
    }
}
