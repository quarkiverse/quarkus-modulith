package io.quarkiverse.modulith.docs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * A diagram: a selection of the components of a {@link DiagramModel} and of the relationships between them, with
 * the semantics of a Structurizr component view. Adding a component adds its relationships to the components already
 * in the view.
 */
final class DiagramView {

    private final DiagramModel model;
    private final Optional<String> title;
    private final TreeMap<Integer, DiagramComponent> elements = new TreeMap<>();
    private final Set<DiagramRelationship> relationships = new HashSet<>();

    DiagramView(DiagramModel model, Optional<String> title) {
        this.model = model;
        this.title = title;
    }

    DiagramModel model() {
        return model;
    }

    Optional<String> title() {
        return title;
    }

    void add(DiagramComponent component) {
        elements.put(component.id(), component);
        for (DiagramRelationship relationship : model.relationships()) {
            boolean outgoing = relationship.source() == component && contains(relationship.destination());
            boolean incoming = relationship.destination() == component && contains(relationship.source());
            if (outgoing || incoming) {
                relationships.add(relationship);
            }
        }
    }

    boolean contains(DiagramComponent component) {
        return elements.get(component.id()) == component;
    }

    void removeRelationshipsWithTag(String tag) {
        relationships.removeIf(it -> it.tag().equals(tag));
    }

    void removeRelationshipsFrom(DiagramComponent component) {
        relationships.removeIf(it -> it.source() == component);
    }

    void remove(DiagramRelationship relationship) {
        relationships.remove(relationship);
    }

    void removeElementsWithNoRelationships() {
        Set<DiagramComponent> connected = new HashSet<>();
        relationships.forEach(it -> {
            connected.add(it.source());
            connected.add(it.destination());
        });
        elements.values().removeIf(it -> !connected.contains(it));
    }

    /**
     * @return the components in identifier order
     */
    List<DiagramComponent> elements() {
        return new ArrayList<>(elements.values());
    }

    /**
     * @return the relationships ordered by identifier compared as text, like Structurizr's exporters
     */
    List<DiagramRelationship> relationships() {
        return relationships.stream().sorted(Comparator.comparing(it -> String.valueOf(it.id()))).toList();
    }
}
