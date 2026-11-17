package io.quarkiverse.modulith.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The violations found in an application. It is an exception so that {@link #throwIfPresent()} and
 * {@link ApplicationModules#verify()} can throw it directly.
 */
public final class Violations extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * No violations.
     */
    public static final Violations NONE = new Violations(List.of());

    private final List<Violation> violations;

    public Violations(List<Violation> violations) {
        super(describe(violations), null, false, false);
        this.violations = List.copyOf(violations);
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public List<Violation> getViolations() {
        return violations;
    }

    /**
     * @return the messages of all violations
     */
    public List<String> getMessages() {
        return violations.stream().map(Violation::message).toList();
    }

    /**
     * @return the violations of the given kind
     */
    public List<Violation> ofKind(Violation.Kind kind) {
        return violations.stream().filter(v -> v.kind() == kind).toList();
    }

    /**
     * @return the violations matching the given predicate
     */
    public Violations filter(Predicate<Violation> filter) {
        return new Violations(violations.stream().filter(filter).toList());
    }

    /**
     * @return these violations and the given one, unless one with the same message is present already
     */
    public Violations and(Violation violation) {
        return and(new Violations(List.of(violation)));
    }

    /**
     * @return these violations and those of the given ones whose messages are not present already
     */
    public Violations and(Violations other) {
        List<Violation> result = new ArrayList<>(violations);
        Set<String> messages = new HashSet<>(getMessages());
        other.violations.stream().filter(it -> !messages.contains(it.message())).forEach(result::add);
        return result.size() == violations.size() ? this : new Violations(result);
    }

    /**
     * Throws this instance if it holds at least one violation.
     */
    public void throwIfPresent() {
        if (hasViolations()) {
            throw this;
        }
    }

    static String describe(List<Violation> violations) {
        return violations.isEmpty() ? "No module violations"
                : violations.stream().map(Violation::message).collect(Collectors.joining("\n- ", "- ", ""));
    }
}
