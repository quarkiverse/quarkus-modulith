package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * How to analyse an application: what {@code @Modulith} would say, plus the {@code quarkus.modulith.*} settings.
 *
 * @param basePackage the base package of the application
 * @param modulithType the type annotated with {@code @Modulith} or {@code @Modulithic}, if any
 * @param systemName the configured system name, overriding the annotation
 * @param sharedModules configured shared modules, in addition to the annotation's
 * @param additionalPackages configured root packages, in addition to the annotation's
 * @param useFullyQualifiedModuleNames the configured value, overriding the annotation
 * @param detectionStrategy {@code direct-sub-packages}, {@code explicitly-annotated}, the name of a strategy class, or
 *        empty for the service loader lookup and then the default
 * @param ignoredPackages package patterns ({@code a.b}, {@code a.b..}, {@code ..b..}) whose classes are left out
 * @param ignoredClasses further classes to leave out
 * @param springAnnotations whether Spring Modulith's annotations count like Quarkus Modulith's
 * @param classLoader loads strategies and source factories, and reads library class files
 * @param allowFieldInjection whether beans of other modules may be injected into fields, which Spring Modulith reports
 *        as violation but is the common style in Quarkus
 */
public record AnalysisSettings(String basePackage, Optional<String> modulithType, Optional<String> systemName,
        List<String> sharedModules, List<String> additionalPackages, Optional<Boolean> useFullyQualifiedModuleNames,
        String detectionStrategy, List<String> ignoredPackages, Predicate<String> ignoredClasses,
        boolean springAnnotations, ClassLoader classLoader, boolean allowFieldInjection) {
}
