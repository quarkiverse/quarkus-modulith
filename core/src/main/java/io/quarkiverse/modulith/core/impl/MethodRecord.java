package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * What the instructions of one method reference. Synthetic lambda and accessor methods are recorded too, and their
 * references are attributed to the method creating the lambda, as ArchUnit does.
 */
final class MethodRecord {

    private static final Pattern SYNTHETIC = Pattern.compile("lambda\\$.*\\$\\d+|access\\$\\d+");

    final String key;
    final String name;
    final String description;
    final boolean hasParameters;
    final List<PendingAccess> accesses = new ArrayList<>();
    final List<ScannedCall> calls = new ArrayList<>();
    final List<String> lambdas = new ArrayList<>();

    MethodRecord(String name, String descriptor, String description, boolean hasParameters) {
        this.key = name + descriptor;
        this.name = name;
        this.description = description;
        this.hasParameters = hasParameters;
    }

    /**
     * @return whether the method is a synthetic lambda body or accessor, which ArchUnit does not import as a member
     */
    static boolean isSynthetic(String name) {
        return SYNTHETIC.matcher(name).matches();
    }

    boolean isSynthetic() {
        return isSynthetic(name);
    }
}
