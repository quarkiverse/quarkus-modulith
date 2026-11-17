package io.quarkiverse.modulith.core.impl;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.core.Violations;

/**
 * Everything that needs ArchUnit: class filters, import options and additional rules. Only loaded when one of these is
 * used, so the rest of the module model works without ArchUnit.
 */
public final class ArchUnitSupport {

    private ArchUnitSupport() {
    }

    public static ApplicationModules load(Class<?> type, DescribedPredicate<? super JavaClass> ignored) {
        return ApplicationModulesLoader.load(type, ignoredClasses(List.of(type.getPackageName()), ignored), Optional.empty(),
                Optional.empty());
    }

    public static ApplicationModules load(String basePackage, DescribedPredicate<? super JavaClass> ignored) {
        return ApplicationModulesLoader.load(basePackage, ignoredClasses(List.of(basePackage), ignored), Optional.empty(),
                Optional.empty());
    }

    public static ApplicationModules load(Class<?> type, ImportOption option) {
        return ApplicationModulesLoader.load(type, ApplicationModulesLoader.NOTHING_IGNORED, Optional.of(locations(option)),
                Optional.empty());
    }

    public static ApplicationModules load(String basePackage, ImportOption option) {
        return ApplicationModulesLoader.load(basePackage, ApplicationModulesLoader.NOTHING_IGNORED,
                Optional.of(locations(option)), Optional.empty());
    }

    private static Predicate<URI> locations(ImportOption option) {
        return it -> option.includes(Location.of(it));
    }

    /**
     * @return the names of the classes of the packages that the predicate matches
     */
    private static Predicate<String> ignoredClasses(List<String> packages, DescribedPredicate<? super JavaClass> ignored) {
        Set<String> names = new ClassFileImporter().importPackages(packages).stream()
                .filter(ignored)
                .map(JavaClass::getName)
                .collect(Collectors.toUnmodifiableSet());
        return names::contains;
    }

    /**
     * @return the jMolecules rules for the libraries on the class path, empty without {@code jmolecules-archunit}
     */
    public static List<ArchRule> jMoleculesRules() {
        return JMoleculesRules.rules(ClassPresence::isPresent);
    }

    /**
     * Imports the analysed classes and evaluates the rules on them.
     *
     * @return one violation per failure detail
     */
    public static Violations evaluate(Collection<ArchRule> rules, ApplicationModules modules) {
        Set<String> analysed = modules.getModelData().types().stream().map(it -> it.name())
                .collect(Collectors.toUnmodifiableSet());
        JavaClasses classes = new ClassFileImporter().importPackages(modules.getModelData().rootPackages())
                .that(DescribedPredicate.describe("analysed by Quarkus Modulith", it -> analysed.contains(it.getName())));
        return new Violations(rules.stream()
                .flatMap(rule -> rule.evaluate(classes).getFailureReport().getDetails().stream())
                .map(it -> new Violation(Violation.Kind.ADDITIONAL_RULE, "", "", "", it))
                .toList());
    }
}
