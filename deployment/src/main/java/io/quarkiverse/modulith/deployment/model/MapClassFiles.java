package io.quarkiverse.modulith.deployment.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.impl.ClassFiles;

/**
 * Class files given by name and a loader function.
 *
 * @param classes the names of the application classes
 * @param bytes loads a class file, may return {@code null}
 */
record MapClassFiles(Collection<String> classes, Function<String, byte[]> bytes) implements ClassFiles {

    @Override
    public Set<String> names(List<String> packages) {
        List<String> prefixes = packages.stream().map(it -> it.isEmpty() ? "" : it + ".").toList();
        return classes.stream().filter(name -> prefixes.stream().anyMatch(name::startsWith))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<byte[]> read(String name) {
        return Optional.ofNullable(bytes.apply(name));
    }
}
