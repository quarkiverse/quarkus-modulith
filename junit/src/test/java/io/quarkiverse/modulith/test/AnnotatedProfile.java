package io.quarkiverse.modulith.test;

@ApplicationModuleTest(value = BootstrapMode.DIRECT_DEPENDENCIES, module = "order", extraIncludes = { "billing",
        "audit" }, verifyAutomatically = false)
public class AnnotatedProfile extends ApplicationModuleTestProfile {
}
