package io.quarkiverse.modulith.events;

import java.util.Objects;
import java.util.Optional;

/**
 * Where an externalized event goes: a target, such as a topic, and an optional key, such as a Kafka record key. Like
 * Spring Modulith's {@code RoutingTarget}, written {@code target} or {@code target::key}.
 * <p>
 * Either part may be an expression evaluated against the event, which Quarkus Modulith compiles at build time when it
 * is declared in {@link Externalized}. An expression part starts with {@code #{} (Spring's form, for example
 * {@code #{#this.getLastname()}}) or with <code>{</code> (Quarkus' form, for example <code>{event.lastname}</code>).
 */
public final class RoutingTarget {

    static final String SEPARATOR = "::";
    private static final String SPRING_EXPRESSION_PREFIX = "#{";
    private static final String EXPRESSION_PREFIX = "{";

    private final String target;
    private final String key;

    private RoutingTarget(String target, String key) {
        if (Objects.isNull(target) || target.isBlank()) {
            throw new IllegalArgumentException("Routing target must not be blank");
        }
        this.target = target.trim();
        this.key = Objects.isNull(key) ? null : key.trim();
    }

    /**
     * @return a routing target without key
     */
    public static RoutingTarget forTarget(String target) {
        return new RoutingTarget(target, null);
    }

    /**
     * Reads {@code target} or {@code target::key}. The target must not be blank, see {@link ParsedRoutingTarget} for
     * declarations that leave it to a fallback.
     */
    public static RoutingTarget parse(String value) {
        return ParsedRoutingTarget.parse(Objects.requireNonNull(value, "value")).toRoutingTarget();
    }

    /**
     * @return a copy with the given key
     */
    public RoutingTarget andKey(String key) {
        return new RoutingTarget(target, Objects.requireNonNull(key, "key"));
    }

    /**
     * @return a copy without key
     */
    public RoutingTarget withoutKey() {
        return new RoutingTarget(target, null);
    }

    /**
     * @return a copy with the given key, or without key when it is {@code null}
     */
    public RoutingTarget withKey(String key) {
        return new RoutingTarget(target, key);
    }

    public String getTarget() {
        return target;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }

    /**
     * @return whether the key is an expression evaluated against the event
     */
    public boolean hasKeyExpression() {
        return Objects.nonNull(key) && isExpression(key);
    }

    /**
     * @return whether the target or the key is an expression evaluated against the event
     */
    public boolean hasExpression() {
        return hasKeyExpression() || isExpression(target);
    }

    /**
     * @return whether the value is an expression, that is, it starts with {@code #{} or <code>{</code>; {@code false}
     *         for {@code null}
     */
    public static boolean isExpression(String value) {
        return Objects.nonNull(value)
                && (value.startsWith(SPRING_EXPRESSION_PREFIX) || value.startsWith(EXPRESSION_PREFIX));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RoutingTarget that && target.equals(that.target) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, key);
    }

    @Override
    public String toString() {
        return Objects.isNull(key) ? target : target + SEPARATOR + key;
    }
}
