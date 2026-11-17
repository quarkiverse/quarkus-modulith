package io.quarkiverse.modulith.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * A test profile that starts one application module, and optionally its dependencies, instead of the whole
 * application. Beans of the other modules are left out at build time. Hibernate ORM entities of the other modules
 * stay mapped, because Quarkus assigns entities to persistence units from the build-time index; to leave them out,
 * set {@code quarkus.hibernate-orm.packages} in {@link #additionalConfigOverrides()}.
 * <p>
 * The settings come from an {@link ApplicationModuleTest} annotation on the profile class or on the class that
 * declares it, and can be overridden by the methods of this class:
 *
 * <pre>
 * package com.acme.inventory;
 *
 * &#64;ApplicationModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
 * &#64;TestProfile(InventoryModuleTest.Profile.class)
 * class InventoryModuleTest {
 *
 *     public static class Profile extends ApplicationModuleTestProfile {
 *     }
 * }
 * </pre>
 *
 * Each profile triggers its own build and application start, so keep the number of profiles small.
 */
public abstract class ApplicationModuleTestProfile implements QuarkusTestProfile {

    /**
     * @return the module under test, as a module identifier or a package inside the module. Defaults to
     *         {@link ApplicationModuleTest#module()}, then to the package of the annotated class, then to the package
     *         of the profile class.
     */
    protected String module() {
        return annotation().map(ApplicationModuleTest::module).filter(it -> !it.isEmpty())
                .orElseGet(() -> annotatedClass().getPackageName());
    }

    /**
     * @return the bootstrap mode, by default the one of the annotation, otherwise {@link BootstrapMode#STANDALONE}
     */
    protected BootstrapMode bootstrapMode() {
        return annotation().map(ApplicationModuleTestProfile::mode).orElse(BootstrapMode.STANDALONE);
    }

    /**
     * @return identifiers of additional modules to start, by default {@link ApplicationModuleTest#extraIncludes()}
     */
    protected List<String> extraModules() {
        return annotation().map(it -> List.of(it.extraIncludes())).orElse(List.of());
    }

    /**
     * @return whether the build fails on any module rule violation, by default
     *         {@link ApplicationModuleTest#verifyAutomatically()}, otherwise {@code true}
     */
    protected boolean verifyAutomatically() {
        return annotation().map(ApplicationModuleTest::verifyAutomatically).orElse(true);
    }

    /**
     * @return further configuration for this test profile
     */
    protected Map<String, String> additionalConfigOverrides() {
        return Map.of();
    }

    @Override
    public final Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>(additionalConfigOverrides());
        config.put("quarkus.modulith.test.module", module());
        config.put("quarkus.modulith.test.bootstrap-mode", bootstrapMode().configValue());
        config.put("quarkus.modulith.test.verify-automatically", String.valueOf(verifyAutomatically()));
        if (!extraModules().isEmpty()) {
            config.put("quarkus.modulith.test.extra-modules", String.join(",", extraModules()));
        }
        return config;
    }

    static BootstrapMode mode(ApplicationModuleTest annotation) {
        return annotation.mode() != BootstrapMode.STANDALONE ? annotation.mode() : annotation.value();
    }

    private Optional<ApplicationModuleTest> annotation() {
        return Optional.ofNullable(annotatedClass().getAnnotation(ApplicationModuleTest.class));
    }

    /**
     * @return the profile class if it is annotated, else the class declaring it if that one is annotated, else the
     *         profile class
     */
    private Class<?> annotatedClass() {
        Class<?> profile = getClass();
        Class<?> declaring = profile.getEnclosingClass();
        if (!profile.isAnnotationPresent(ApplicationModuleTest.class) && Objects.nonNull(declaring)
                && declaring.isAnnotationPresent(ApplicationModuleTest.class)) {
            return declaring;
        }
        return profile;
    }
}
