package io.quarkiverse.modulith.events;

import java.util.Objects;
import java.util.Optional;

/**
 * A routing target as declared, for example in {@link Externalized}, where the target or the key may be missing. Like
 * Spring Modulith's {@code RoutingTarget.ParsedRoutingTarget}: {@code ::key} declares only a key and leaves the target
 * to a fallback.
 */
public final class ParsedRoutingTarget {

    private final String target;
    private final String key;

    private ParsedRoutingTarget(String target, String key) {
        this.target = target;
        this.key = key;
    }

    /**
     * Reads {@code target}, {@code target::key} or {@code ::key}. Blank parts count as missing, except an explicitly
     * declared empty key.
     */
    public static ParsedRoutingTarget parse(String source) {
        Objects.requireNonNull(source, "source");
        int separator = source.indexOf(RoutingTarget.SEPARATOR);
        String target = separator < 0 ? source : source.substring(0, separator);
        String key = separator < 0 ? null : source.substring(separator + RoutingTarget.SEPARATOR.length()).trim();
        return new ParsedRoutingTarget(target.isBlank() ? null : target.trim(), key);
    }

    public Optional<String> getTarget() {
        return Optional.ofNullable(target);
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }

    /**
     * @return the routing target
     * @throws IllegalArgumentException if no target was declared
     */
    public RoutingTarget toRoutingTarget() {
        return RoutingTarget.forTarget(target).withKey(key);
    }

    /**
     * @return the routing target, taking the target and the key from the fallback where they were not declared
     */
    public RoutingTarget withFallback(RoutingTarget fallback) {
        return RoutingTarget.forTarget(getTarget().orElse(fallback.getTarget()))
                .withKey(getKey().orElse(fallback.getKey().orElse(null)));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ParsedRoutingTarget that && Objects.equals(target, that.target)
                && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, key);
    }

    @Override
    public String toString() {
        return getTarget().orElse("") + RoutingTarget.SEPARATOR + getKey().orElse("");
    }
}
