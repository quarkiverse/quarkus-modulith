package io.quarkiverse.modulith.test;

import java.util.Map;

@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES, value = BootstrapMode.DIRECT_DEPENDENCIES)
public class ModeProfile extends ApplicationModuleTestProfile {

    @Override
    protected Map<String, String> additionalConfigOverrides() {
        return Map.of("quarkus.log.level", "DEBUG");
    }
}
