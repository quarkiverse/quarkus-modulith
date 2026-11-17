package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * Module information read from the module declaration on a package. jMolecules' {@code @Module} only names the module
 * and never restricts dependencies.
 */
final class AnnotationModuleInformation implements ApplicationModuleInformation {

    static final String JMOLECULES_MODULE = "org.jmolecules.ddd.annotation.Module";
    static final Set<String> MODULE_ANNOTATIONS = Set.of("io.quarkiverse.modulith.ApplicationModule",
            "org.springframework.modulith.ApplicationModule");
    static final Set<String> ALL_ANNOTATIONS = Set.of("io.quarkiverse.modulith.ApplicationModule",
            "org.springframework.modulith.ApplicationModule", JMOLECULES_MODULE);

    private final Optional<AnnotationData> annotation;

    private AnnotationModuleInformation(Optional<AnnotationData> annotation) {
        this.annotation = annotation;
    }

    static AnnotationModuleInformation of(JavaPackage javaPackage) {
        JavaPackage single = javaPackage.toSingle();
        Optional<AnnotationData> jMolecules = single.findDeclaration(Set.of(JMOLECULES_MODULE));
        return jMolecules.isPresent() ? new AnnotationModuleInformation(jMolecules)
                : new AnnotationModuleInformation(single.findDeclaration(MODULE_ANNOTATIONS));
    }

    private boolean isJMolecules() {
        return annotation.filter(it -> it.type().equals(JMOLECULES_MODULE)).isPresent();
    }

    @Override
    public Optional<String> getDisplayName() {
        Optional<String> displayName = annotation.flatMap(it -> it.value(isJMolecules() ? "name" : "displayName"))
                .filter(it -> !it.isBlank());
        return isJMolecules() ? displayName.or(() -> annotation.flatMap(it -> it.value("value")).filter(it -> !it.isBlank()))
                : displayName;
    }

    @Override
    public List<String> getDeclaredDependencies() {
        return isJMolecules() ? List.of(OPEN_TOKEN)
                : annotation.flatMap(it -> it.values("allowedDependencies")).orElse(List.of(OPEN_TOKEN));
    }

    @Override
    public boolean isOpen() {
        return annotation.flatMap(it -> it.value("type")).filter("OPEN"::equals).isPresent();
    }

    /**
     * @return the declared module identifier, if any
     */
    Optional<String> getIdentifier() {
        return annotation.flatMap(it -> it.value("id")).filter(it -> !it.isBlank());
    }
}
