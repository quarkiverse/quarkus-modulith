package io.quarkiverse.modulith.core.data;

/**
 * A reference from a type of one module to a type of another module.
 *
 * @param source the name of the referencing type
 * @param target the name of the referenced type
 * @param type the name of the {@code DependencyType}
 * @param description where and how the reference is made
 * @param injectedField the full name ({@code com.acme.Type.field}) of the field the target is injected into, outside a
 *        configuration class, empty otherwise
 */
public record DependencyData(String source, String target, String type, String description, String injectedField) {
}
