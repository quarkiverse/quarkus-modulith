package io.quarkiverse.modulith.runtime.initializer;

import java.util.List;

/**
 * The initializer bean classes in the order they run, computed at build time from the module dependency order.
 *
 * @param classNames the names of the initializer bean classes, initializers of modules others depend on first
 */
public record InitializerOrder(List<String> classNames) {

    /**
     * @return the position of the bean class, initializers that were not known at build time last
     */
    int indexOf(Class<?> beanClass) {
        int index = classNames.indexOf(beanClass.getName());
        return index < 0 ? classNames.size() : index;
    }
}
