package io.quarkiverse.modulith.core.impl;

import java.util.Set;

/**
 * The names of the annotations and types the analysis recognizes. The analysis reads them from class files, so
 * none of these libraries has to be on the class path.
 */
final class Names {

    static final String SPRING_MODULITH = "org.springframework.modulith.";
    static final Set<String> MODULITH = Set.of("io.quarkiverse.modulith.Modulith", "io.quarkiverse.modulith.Modulithic",
            "org.springframework.modulith.Modulith", "org.springframework.modulith.Modulithic");
    static final Set<String> APPLICATION_MODULE = Set.of("io.quarkiverse.modulith.ApplicationModule",
            "org.springframework.modulith.ApplicationModule");
    static final String JMOLECULES_MODULE = "org.jmolecules.ddd.annotation.Module";
    static final Set<String> NAMED_INTERFACE = Set.of("io.quarkiverse.modulith.NamedInterface",
            "org.springframework.modulith.NamedInterface");
    static final Set<String> MODULE_LISTENER = Set.of("io.quarkiverse.modulith.ApplicationModuleListener",
            "org.springframework.modulith.events.ApplicationModuleListener");

    static final Set<String> INJECT = Set.of("jakarta.inject.Inject", "jakarta.annotation.Resource",
            "org.springframework.beans.factory.annotation.Autowired");
    static final Set<String> OBSERVES = Set.of("jakarta.enterprise.event.Observes",
            "jakarta.enterprise.event.ObservesAsync");
    static final String OBSERVES_ASYNC = "jakarta.enterprise.event.ObservesAsync";
    static final String EVENT = "jakarta.enterprise.event.Event";
    static final Set<String> SPRING_EVENT_LISTENER = Set.of("org.springframework.context.event.EventListener",
            "org.springframework.transaction.event.TransactionalEventListener");
    static final String TRANSACTIONAL_EVENT_LISTENER = "org.springframework.transaction.event.TransactionalEventListener";
    static final String APPLICATION_LISTENER = "org.springframework.context.ApplicationListener";
    static final Set<String> SPRING_ASYNC = Set.of("org.springframework.scheduling.annotation.Async");
    static final Set<String> DOMAIN_EVENT_HANDLER = Set.of("org.jmolecules.event.annotation.DomainEventHandler");

    static final Set<String> SCOPES = Set.of("jakarta.enterprise.context.NormalScope", "jakarta.inject.Scope",
            "jakarta.enterprise.inject.Stereotype", "jakarta.ws.rs.Path", "org.springframework.stereotype.Component");
    static final Set<String> PRODUCER = Set.of("jakarta.enterprise.inject.Produces",
            "org.springframework.context.annotation.Bean");
    static final Set<String> CONFIGURATION = Set.of("org.springframework.context.annotation.Configuration");
    static final Set<String> CONFIGURATION_PROPERTIES = Set.of(
            "org.springframework.boot.context.properties.ConfigurationProperties", "io.smallrye.config.ConfigMapping");
    static final String CONSTRAINT_VALIDATOR = "jakarta.validation.ConstraintValidator";
    static final Set<String> SPRING_DATA_REPOSITORY = Set.of("org.springframework.data.repository.Repository");
    static final Set<String> REPOSITORY_DEFINITION = Set.of(
            "org.springframework.data.repository.RepositoryDefinition", "jakarta.data.repository.Repository");
    static final Set<String> PANACHE_REPOSITORY = Set.of("io.quarkus.hibernate.orm.panache.PanacheRepositoryBase",
            "io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase",
            "io.quarkus.mongodb.panache.PanacheMongoRepositoryBase");
    static final Set<String> PANACHE_ENTITY = Set.of("io.quarkus.hibernate.orm.panache.PanacheEntityBase",
            "io.quarkus.hibernate.reactive.panache.PanacheEntityBase", "io.quarkus.mongodb.panache.PanacheMongoEntityBase");

    static final String JPA_ENTITY = "jakarta.persistence.Entity";
    static final Set<String> ENTITY = Set.of(JPA_ENTITY, "org.springframework.data.mongodb.core.mapping.Document",
            "org.jmolecules.ddd.annotation.Entity");
    static final Set<String> ENTITY_TYPE = Set.of("org.jmolecules.ddd.types.Entity");
    static final Set<String> AGGREGATE_ROOT = Set.of("org.jmolecules.ddd.annotation.AggregateRoot");
    static final Set<String> AGGREGATE_ROOT_TYPE = Set.of("org.jmolecules.ddd.types.AggregateRoot");
    static final Set<String> REPOSITORY = Set.of("org.springframework.stereotype.Repository",
            "org.jmolecules.ddd.annotation.Repository", "jakarta.data.repository.Repository");
    static final Set<String> REPOSITORY_TYPE = Set.of("org.jmolecules.ddd.types.Repository",
            "org.springframework.data.repository.Repository", "io.quarkus.hibernate.orm.panache.PanacheRepositoryBase",
            "io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase",
            "io.quarkus.mongodb.panache.PanacheMongoRepositoryBase");
    static final Set<String> SERVICE = Set.of("org.springframework.stereotype.Service",
            "org.jmolecules.ddd.annotation.Service");
    static final Set<String> CONTROLLER = Set.of("org.springframework.stereotype.Controller", "jakarta.ws.rs.Path",
            "org.springframework.data.rest.webmvc.BasePathAwareController");
    static final Set<String> VALUE_OBJECT = Set.of("org.jmolecules.ddd.annotation.ValueObject");
    static final Set<String> VALUE_OBJECT_TYPE = Set.of("org.jmolecules.ddd.types.ValueObject");
    static final Set<String> DOMAIN_EVENT = Set.of("org.jmolecules.event.annotation.DomainEvent");
    static final String DOMAIN_EVENT_TYPE = "org.jmolecules.event.types.DomainEvent";

    static final String HTTP_METHOD = "jakarta.ws.rs.HttpMethod";
    static final String PATH = "jakarta.ws.rs.Path";
    static final Set<String> ROUTE = Set.of("io.quarkus.vertx.web.Route");
    static final Set<String> REQUEST_MAPPING = Set.of("org.springframework.web.bind.annotation.RequestMapping");
    static final Set<String> INCOMING = Set.of("org.eclipse.microprofile.reactive.messaging.Incoming",
            "org.springframework.kafka.annotation.KafkaListener",
            "org.springframework.amqp.rabbit.annotation.RabbitListener",
            "org.springframework.jms.annotation.JmsListener");
    static final Set<String> SCHEDULED = Set.of("io.quarkus.scheduler.Scheduled",
            "org.springframework.scheduling.annotation.Scheduled");

    private Names() {
    }
}
