package io.quarkiverse.modulith.core;

import java.util.Arrays;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * ArchUnit predicates for {@link ApplicationModules#of(Class, DescribedPredicate)}, like Spring Modulith's
 * {@code ApplicationModules.Filters}.
 */
public final class Filters {

    private Filters() {
    }

    /**
     * @return a predicate matching the classes of every package with one of the given names, at any depth
     */
    public static DescribedPredicate<JavaClass> withoutModules(String... names) {
        return Arrays.stream(names).map(Filters::withoutModule)
                .reduce(DescribedPredicate.<JavaClass> alwaysFalse(),
                        (left, right) -> left.<JavaClass> or(right));
    }

    /**
     * @return a predicate matching the classes of every package with the given name, at any depth
     */
    public static DescribedPredicate<JavaClass> withoutModule(String name) {
        return JavaClass.Predicates.resideInAPackage(".." + name + "..");
    }
}
