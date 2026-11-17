package io.quarkiverse.modulith.deployment.docs;

import java.util.Map;

import io.quarkiverse.modulith.docs.DocumentationOutput;

/**
 * Collects the documenter's files in a map, so the build hands them to Quarkus instead of writing them itself.
 */
record MapOutput(Map<String, String> files) implements DocumentationOutput {

    @Override
    public boolean contains(String name) {
        return files.containsKey(name);
    }

    @Override
    public void write(String name, String content) {
        files.put(name, content);
    }

    @Override
    public void clear() {
        files.clear();
    }
}
