package io.quarkiverse.modulith.deployment.docs;

import java.util.List;

import org.jboss.jandex.DotName;

/**
 * The annotation and type names the documentation model looks for in the index. None of these libraries has to be
 * on the class path.
 */
final class Names {

    static final DotName SPRING_MODULE_LISTENER = DotName
            .createSimple("org.springframework.modulith.events.ApplicationModuleListener");
    static final DotName SPRING_EVENT_LISTENER = DotName.createSimple("org.springframework.context.event.EventListener");
    static final DotName ASYNC = DotName.createSimple("org.springframework.scheduling.annotation.Async");
    static final DotName OBSERVES = DotName.createSimple("jakarta.enterprise.event.Observes");
    static final DotName OBSERVES_ASYNC = DotName.createSimple("jakarta.enterprise.event.ObservesAsync");
    static final DotName INJECT = DotName.createSimple("jakarta.inject.Inject");
    static final DotName STEREOTYPE = DotName.createSimple("jakarta.enterprise.inject.Stereotype");
    static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");
    static final DotName ROUTE = DotName.createSimple("io.quarkus.vertx.web.Route");
    static final DotName ROUTE_BASE = DotName.createSimple("io.quarkus.vertx.web.RouteBase");
    static final DotName SCHEDULED = DotName.createSimple("io.quarkus.scheduler.Scheduled");
    static final DotName INCOMING = DotName.createSimple("org.eclipse.microprofile.reactive.messaging.Incoming");
    static final DotName OUTGOING = DotName.createSimple("org.eclipse.microprofile.reactive.messaging.Outgoing");
    static final DotName CONFIG_MAPPING = DotName.createSimple("io.smallrye.config.ConfigMapping");
    static final DotName CONFIG_PROPERTY = DotName.createSimple("org.eclipse.microprofile.config.inject.ConfigProperty");
    static final DotName WITH_NAME = DotName.createSimple("io.smallrye.config.WithName");
    static final DotName WITH_PARENT_NAME = DotName.createSimple("io.smallrye.config.WithParentName");
    static final DotName WITH_DEFAULT = DotName.createSimple("io.smallrye.config.WithDefault");
    static final DotName ENTITY = DotName.createSimple("jakarta.persistence.Entity");
    static final DotName EMBEDDABLE = DotName.createSimple("jakarta.persistence.Embeddable");
    static final DotName JAKARTA_DATA_REPOSITORY = DotName.createSimple("jakarta.data.repository.Repository");
    static final DotName JMOLECULES_SERVICE = DotName.createSimple("org.jmolecules.ddd.annotation.Service");
    static final DotName JMOLECULES_REPOSITORY = DotName.createSimple("org.jmolecules.ddd.annotation.Repository");
    static final DotName JMOLECULES_AGGREGATE_ROOT = DotName.createSimple("org.jmolecules.ddd.annotation.AggregateRoot");
    static final DotName JMOLECULES_VALUE_OBJECT = DotName.createSimple("org.jmolecules.ddd.annotation.ValueObject");
    static final DotName APPLICATION_MODULE_INITIALIZER = DotName
            .createSimple("io.quarkiverse.modulith.ApplicationModuleInitializer");

    static final String UNCONFIGURED_VALUE = "org.eclipse.microprofile.config.configproperty.unconfigureddvalue";

    static final List<DotName> BEAN_DEFINING = List.of(
            DotName.createSimple("jakarta.enterprise.context.ApplicationScoped"),
            DotName.createSimple("jakarta.enterprise.context.RequestScoped"),
            DotName.createSimple("jakarta.enterprise.context.SessionScoped"),
            DotName.createSimple("jakarta.enterprise.context.Dependent"),
            DotName.createSimple("jakarta.inject.Singleton"));

    static final List<String> REPOSITORY_TYPES = List.of(
            "io.quarkus.hibernate.orm.panache.PanacheRepositoryBase",
            "io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase",
            "io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase",
            "io.quarkus.mongodb.panache.PanacheMongoRepositoryBase",
            "io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase",
            "org.jmolecules.ddd.types.Repository",
            "org.springframework.data.repository.Repository");

    static final List<String> PANACHE_ENTITIES = List.of(
            "io.quarkus.hibernate.orm.panache.PanacheEntityBase",
            "io.quarkus.hibernate.reactive.panache.PanacheEntityBase",
            "io.quarkus.mongodb.panache.PanacheMongoEntityBase");

    static final List<String> INSTANCE_TYPES = List.of("jakarta.enterprise.inject.Instance",
            "jakarta.inject.Provider", "io.quarkus.arc.InjectableInstance");

    private Names() {
    }
}
