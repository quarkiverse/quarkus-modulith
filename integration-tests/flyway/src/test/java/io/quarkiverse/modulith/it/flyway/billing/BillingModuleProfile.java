package io.quarkiverse.modulith.it.flyway.billing;

import io.quarkiverse.modulith.test.ApplicationModuleTestProfile;
import io.quarkiverse.modulith.test.BootstrapMode;

public class BillingModuleProfile extends ApplicationModuleTestProfile {

    @Override
    protected BootstrapMode bootstrapMode() {
        return BootstrapMode.DIRECT_DEPENDENCIES;
    }
}
