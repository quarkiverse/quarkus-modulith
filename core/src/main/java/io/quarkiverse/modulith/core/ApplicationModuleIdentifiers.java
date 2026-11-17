package io.quarkiverse.modulith.core;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Module identifiers that iterate in the order of their source, like Spring Modulith's
 * {@code ApplicationModuleIdentifiers}.
 */
public final class ApplicationModuleIdentifiers implements Iterable<ApplicationModuleIdentifier> {

    private final Supplier<Stream<ApplicationModuleIdentifier>> source;

    private ApplicationModuleIdentifiers(Supplier<Stream<ApplicationModuleIdentifier>> source) {
        this.source = source;
    }

    /**
     * @return the identifiers of the given modules, in dependency order
     */
    public static ApplicationModuleIdentifiers of(ApplicationModules modules) {
        return new ApplicationModuleIdentifiers(() -> modules.stream().map(ApplicationModule::getIdentifier));
    }

    public static ApplicationModuleIdentifiers of(List<ApplicationModuleIdentifier> identifiers) {
        return new ApplicationModuleIdentifiers(identifiers::stream);
    }

    public Stream<ApplicationModuleIdentifier> stream() {
        return source.get();
    }

    @Override
    public Iterator<ApplicationModuleIdentifier> iterator() {
        return stream().iterator();
    }
}
