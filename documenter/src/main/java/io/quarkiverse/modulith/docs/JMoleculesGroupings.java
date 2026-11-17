package io.quarkiverse.modulith.docs;

import java.util.function.Predicate;

/**
 * The bean groups for the jMolecules architecture annotations (CQRS, hexagonal, layered and onion), Spring Modulith's
 * {@code Groupings.JMoleculesGroupings}. The annotations are matched by name on the bean class, its annotations and
 * its package, so jMolecules does not have to be on the class path of the documenter.
 */
public final class JMoleculesGroupings {

    private static final String CQRS = "org.jmolecules.architecture.cqrs.";
    private static final String HEXAGONAL = "org.jmolecules.architecture.hexagonal.";
    private static final String LAYERED = "org.jmolecules.architecture.layered.";
    private static final String CLASSICAL = "org.jmolecules.architecture.onion.classical.";
    private static final String SIMPLIFIED = "org.jmolecules.architecture.onion.simplified.";

    private JMoleculesGroupings() {
    }

    public static Grouping[] getGroupings() {
        return new Grouping[] {
                group("Commands", CQRS + "Command"),
                group("Command dispatchers", CQRS + "CommandDispatcher"),
                group("Command handlers", CQRS + "CommandHandler"),
                group("Query models", CQRS + "QueryModel"),
                group("Primary ports", HEXAGONAL + "PrimaryPort"),
                group("Secondary ports", HEXAGONAL + "SecondaryPort"),
                group("Ports", HEXAGONAL + "Port"),
                group("Application", HEXAGONAL + "Application"),
                group("Primary adapters", HEXAGONAL + "PrimaryAdapter"),
                group("Secondary adapters", HEXAGONAL + "SecondaryAdapter"),
                group("Adapters", HEXAGONAL + "Adapter"),
                group("Application layer", LAYERED + "ApplicationLayer"),
                group("Domain layer", LAYERED + "DomainLayer"),
                group("Infrastructure layer", LAYERED + "InfrastructureLayer"),
                group("Interface layer", LAYERED + "InterfaceLayer"),
                group("Application ring", SIMPLIFIED + "ApplicationRing"),
                group("Domain ring", SIMPLIFIED + "DomainRing"),
                group("Infrastructure ring", SIMPLIFIED + "InfrastructureRing"),
                group("Application service ring", CLASSICAL + "ApplicationServiceRing"),
                group("Domain service ring", CLASSICAL + "DomainServiceRing"),
                group("Domain model ring", CLASSICAL + "DomainModelRing"),
                group("Infrastructure ring", CLASSICAL + "InfrastructureRing")
        };
    }

    private static Grouping group(String name, String annotation) {
        return Grouping.of(name, packageOrTypeAnnotatedWith(annotation));
    }

    static Predicate<ModuleBean> packageOrTypeAnnotatedWith(String annotation) {
        return bean -> bean.type().isAnnotatedWith(annotation) || bean.type().isMetaAnnotatedWith(annotation)
                || bean.type().isPackageAnnotatedWith(annotation);
    }
}
