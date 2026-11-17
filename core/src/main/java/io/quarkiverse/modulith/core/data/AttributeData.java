package io.quarkiverse.modulith.core.data;

import java.util.List;

/**
 * One attribute of a recorded annotation.
 *
 * @param name the attribute name
 * @param values the attribute values as text: strings as they are, enum constants by name, classes by name
 */
public record AttributeData(String name, List<String> values) {
}
