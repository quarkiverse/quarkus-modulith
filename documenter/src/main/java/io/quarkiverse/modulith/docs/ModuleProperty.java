package io.quarkiverse.modulith.docs;

import java.util.Optional;

/**
 * A configuration property declared by a module.
 *
 * @param name the property name, for example {@code shop.order.limit}
 * @param type the type of the value
 * @param sourceType the binary name of the type declaring the property
 * @param description the description of the property, from its Javadoc
 * @param defaultValue the default value
 */
public record ModuleProperty(String name, String type, String sourceType, Optional<String> description,
        Optional<String> defaultValue) {
}
