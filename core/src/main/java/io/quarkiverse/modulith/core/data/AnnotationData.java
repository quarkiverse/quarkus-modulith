package io.quarkiverse.modulith.core.data;

import java.util.List;
import java.util.Optional;

/**
 * An annotation that declares something about the module structure ({@code @ApplicationModule}, {@code @NamedInterface},
 * {@code @Modulith} and their Spring Modulith and jMolecules counterparts), with its explicitly set attributes.
 *
 * @param type the fully qualified name of the annotation type
 * @param attributes the attributes set in the source code, attributes left at their default are absent
 */
public record AnnotationData(String type, List<AttributeData> attributes) {

    /**
     * @return the values of the attribute, if it was set explicitly
     */
    public Optional<List<String>> values(String name) {
        return attributes.stream().filter(it -> it.name().equals(name)).map(AttributeData::values).findFirst();
    }

    /**
     * @return the first value of the attribute, if it was set explicitly to a non-empty value
     */
    public Optional<String> value(String name) {
        return values(name).flatMap(it -> it.stream().findFirst());
    }
}
