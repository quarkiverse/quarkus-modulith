package io.quarkiverse.modulith.docs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkiverse.modulith.core.ApplicationModule;

/**
 * The elements every diagram is drawn from: one component per module, the relationships between them and the
 * background colors. Mirrors the Structurizr workspace Spring Modulith builds, including its element identifiers: the
 * software system is {@code 1}, the container {@code 2}, components and relationships follow in creation order.
 */
final class DiagramModel {

    private static final int FIRST_COMPONENT_ID = 3;

    private final String systemName;
    private final Map<String, DiagramComponent> components = new LinkedHashMap<>();
    private final List<DiagramRelationship> relationships = new ArrayList<>();
    private final Map<String, String> backgrounds = new LinkedHashMap<>();
    private int nextId = FIRST_COMPONENT_ID;

    DiagramModel(String systemName) {
        this.systemName = systemName;
    }

    String systemName() {
        return systemName;
    }

    DiagramComponent addComponent(ApplicationModule module, String name) {
        boolean exists = components.values().stream().anyMatch(it -> it.getName().equals(name));
        if (exists) {
            throw new IllegalArgumentException("A component named '" + name + "' already exists for this container.");
        }
        DiagramComponent component = new DiagramComponent(nextId++, name, module);
        components.put(module.getName(), component);
        return component;
    }

    DiagramComponent component(ApplicationModule module) {
        DiagramComponent component = components.get(module.getName());
        if (Objects.isNull(component)) {
            throw new IllegalStateException("Couldn't find component for module %s!".formatted(module));
        }
        return component;
    }

    /**
     * Adds a relationship unless the source already has one to the destination with the same description.
     *
     * @return the new relationship, empty when it existed
     */
    Optional<DiagramRelationship> uses(DiagramComponent source, DiagramComponent destination, String description,
            String tag) {
        boolean exists = relationships.stream().anyMatch(it -> it.source() == source
                && it.destination() == destination && it.description().equals(description));
        if (exists) {
            return Optional.empty();
        }
        DiagramRelationship relationship = new DiagramRelationship(nextId++, source, destination, description, tag);
        relationships.add(relationship);
        return Optional.of(relationship);
    }

    List<DiagramRelationship> relationships() {
        return Collections.unmodifiableList(relationships);
    }

    /**
     * Tags the component with {@code <module>-<color>} and sets the background of that tag.
     */
    void color(DiagramComponent component, String color) {
        String tag = component.getModule().getName() + "-" + color;
        String hex = Colors.toHex(color);
        component.addTag(tag);
        backgrounds.put(tag, hex);
    }

    /**
     * @return the background of the last tag of the component that has one
     */
    Optional<String> background(DiagramComponent component) {
        String result = null;
        for (String tag : component.getTags()) {
            result = backgrounds.getOrDefault(tag, result);
        }
        return Optional.ofNullable(result);
    }

    /**
     * @return the tags that make up the component's style: {@code Element,Component} and the colored tags
     */
    String styleName(DiagramComponent component) {
        StringBuilder name = new StringBuilder("Element,Component");
        component.getTags().stream().filter(backgrounds::containsKey).forEach(tag -> name.append(',').append(tag));
        return name.toString();
    }
}
