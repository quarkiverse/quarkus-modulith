package io.quarkiverse.modulith.deployment.docs;

/**
 * How one module relates to another in the Mermaid diagrams and the Dev UI.
 */
public enum Relation {

    USES("uses"),
    LISTENS_TO("listens to");

    private final String label;

    Relation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
