package io.quarkiverse.modulith.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The {@code quarkus.modulith.*} build properties for a model built in a plain test: those of the first
 * {@code application.properties} on the class path, overridden by system properties.
 */
record ModulithProperties(Map<String, String> values) {

    static final String PREFIX = "quarkus.modulith.";

    static ModulithProperties load(ClassLoader classLoader) {
        Properties properties = new Properties();
        Optional.ofNullable(classLoader.getResourceAsStream("application.properties")).ifPresent(it -> read(properties, it));
        Map<String, String> values = new TreeMap<>();
        Stream.of(properties, System.getProperties()).forEach(source -> source.stringPropertyNames().stream()
                .filter(it -> it.startsWith(PREFIX))
                .forEach(it -> values.put(it, source.getProperty(it).trim())));
        return new ModulithProperties(Map.copyOf(values));
    }

    static void read(Properties properties, InputStream input) {
        try (input) {
            properties.load(input);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Optional<String> get(String key) {
        return Optional.ofNullable(values.get(PREFIX + key)).filter(it -> !it.isEmpty());
    }

    List<String> list(String key) {
        return get(key).stream().flatMap(it -> Stream.of(it.split(","))).map(String::trim)
                .filter(it -> !it.isEmpty()).toList();
    }

    boolean flag(String key, boolean defaultValue) {
        return get(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /**
     * @return the analysis settings these properties describe
     */
    AnalysisSettings settings(String basePackage, Optional<String> modulithType, Predicate<String> ignored,
            ClassLoader classLoader) {
        return new AnalysisSettings(basePackage, modulithType, get("system-name"), list("shared-modules"),
                list("additional-packages"), get("use-fully-qualified-module-names").map(Boolean::parseBoolean),
                get("detection-strategy").orElse(""), list("ignored-packages"), ignored,
                flag("spring-annotations.enabled", true), classLoader, flag("verification.allow-field-injection", true));
    }
}
