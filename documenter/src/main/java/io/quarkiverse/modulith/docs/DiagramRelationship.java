package io.quarkiverse.modulith.docs;

/**
 * A relationship between two module components, tagged with the dependency type it stands for.
 */
record DiagramRelationship(int id, DiagramComponent source, DiagramComponent destination, String description,
        String tag) {
}
