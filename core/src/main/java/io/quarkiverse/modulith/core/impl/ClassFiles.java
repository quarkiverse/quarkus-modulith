package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The class files of an application.
 */
public interface ClassFiles {

    /**
     * @param packages package names
     * @return the binary names of the application classes in the given packages and their sub-packages
     */
    Set<String> names(List<String> packages);

    /**
     * @return the class file of an application class
     */
    Optional<byte[]> read(String name);
}
