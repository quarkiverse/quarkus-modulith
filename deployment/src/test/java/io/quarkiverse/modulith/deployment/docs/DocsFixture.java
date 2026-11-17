package io.quarkiverse.modulith.deployment.docs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.modulith.deployment.model.Fixtures;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;
import io.quarkiverse.modulith.docs.metadata.MethodMetadata;
import io.quarkiverse.modulith.docs.metadata.TypeMetadata;

/**
 * The documentation fixture: the compiled classes of {@code fixtures.app} plus generated classes for libraries the
 * tests do not have.
 */
public final class DocsFixture {

    static final String BASE = "io.quarkiverse.modulith.deployment.docs.fixtures.app";
    static final String WEB = BASE + ".web.";
    static final String ORDER = BASE + ".order.";

    final Map<String, byte[]> classes;
    final Index index;
    public final ModuleModel model;

    public DocsFixture() {
        classes = new HashMap<>(Fixtures.classes(BASE));
        generated().forEach(it -> classes.put(it.className(), it.bytes()));
        model = Fixtures.model(classes, Optional.of(BASE), Set.of());
        Indexer indexer = new Indexer();
        classes.values().forEach(bytes -> {
            try {
                indexer.index(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        index = indexer.complete();
    }

    public DocumentationModel build() {
        DocumentationSource javadoc = DocumentationSource.of(List.of(new TypeMetadata(ORDER + "OrderConfig",
                Optional.empty(), List.of(new MethodMetadata("maxSize", "maxSize()", Optional.of("At most."))))));
        return new DocumentationModelBuilder(index).build(model, model.applicationModules(), javadoc);
    }

    static List<GeneratedClass> generated() {
        String completed = "L" + (ORDER + "OrderCompleted").replace('.', '/') + ";";
        String owner = "L" + (ORDER + "ItemOwner").replace('.', '/') + ";";
        return List.of(
                GeneratedClass.named(WEB + "Resource").annotatedWith("jakarta.ws.rs.Path"),
                GeneratedClass.named(WEB + "Routes").method("hello", "()V", "io.quarkus.vertx.web.Route"),
                GeneratedClass.named(WEB + "RouteBased").annotatedWith("io.quarkus.vertx.web.RouteBase",
                        "jakarta.inject.Singleton"),
                GeneratedClass.named(WEB + "MethodPath").annotatedWith("jakarta.enterprise.context.ApplicationScoped")
                        .method("get", "()V", "jakarta.ws.rs.Path"),
                GeneratedClass.named(WEB + "Jobs").method("run", "()V", "io.quarkus.scheduler.Scheduled"),
                GeneratedClass.named(WEB + "Consumer").method("consume", "(Ljava/lang/String;)V",
                        "org.eclipse.microprofile.reactive.messaging.Incoming"),
                GeneratedClass.named(WEB + "Producer").method("produce", "()V",
                        "org.eclipse.microprofile.reactive.messaging.Outgoing"),
                GeneratedClass.named(WEB + "Billing").annotatedWith("org.jmolecules.ddd.annotation.Service",
                        "jakarta.enterprise.context.ApplicationScoped"),
                GeneratedClass.named(WEB + "MyService").access(Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE
                        | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ANNOTATION)
                        .implementing(null, "java.lang.annotation.Annotation")
                        .annotatedWith("org.jmolecules.ddd.annotation.Service"),
                GeneratedClass.named(WEB + "MetaService").annotatedWith(WEB + "MyService",
                        "jakarta.enterprise.context.ApplicationScoped"),
                GeneratedClass.named(WEB + "Repo1").annotatedWith("org.jmolecules.ddd.annotation.Repository",
                        "jakarta.enterprise.context.ApplicationScoped"),
                GeneratedClass.named(WEB + "Repo2").annotatedWith("jakarta.data.repository.Repository",
                        "jakarta.enterprise.context.ApplicationScoped"),
                GeneratedClass.named(WEB + "Repo3").annotatedWith("jakarta.enterprise.context.ApplicationScoped")
                        .implementing("Ljava/lang/Object;Lio/quarkus/hibernate/orm/panache/PanacheRepositoryBase<L"
                                + (WEB + "Item").replace('.', '/') + ";Ljava/lang/Long;>;Ljava/io/Serializable;",
                                "io.quarkus.hibernate.orm.panache.PanacheRepositoryBase", "java.io.Serializable"),
                GeneratedClass.named(WEB + "Item").annotatedWith("jakarta.persistence.Entity")
                        .field("owner", owner, null)
                        .field("owners", "Ljava/util/List;", "Ljava/util/List<" + owner + ">;")
                        .field("count", "I", null),
                GeneratedClass.named(ORDER + "ItemOwner").annotatedWith("jakarta.persistence.Entity"),
                GeneratedClass.named(WEB + "Active").annotatedWith("jakarta.persistence.Entity")
                        .extending("io.quarkus.hibernate.orm.panache.PanacheEntityBase"),
                GeneratedClass.named(WEB + "Plain").annotatedWith("jakarta.persistence.Entity"),
                GeneratedClass.named(WEB + "Root").annotatedWith("org.jmolecules.ddd.annotation.AggregateRoot"),
                GeneratedClass.named(WEB + "Root2").implementing(null, "org.jmolecules.ddd.types.AggregateRoot"),
                GeneratedClass.named(WEB + "Value").annotatedWith("org.jmolecules.ddd.annotation.ValueObject"),
                GeneratedClass.named(WEB + "Value2").implementing(null, "org.jmolecules.ddd.types.ValueObject"),
                GeneratedClass.named(WEB + "Embedded").annotatedWith("jakarta.persistence.Embeddable"),
                GeneratedClass.named(WEB + "SpringListener")
                        .method("module", "(" + completed + ")V",
                                "org.springframework.modulith.events.ApplicationModuleListener")
                        .method("plain", "(" + completed + "[Ljava/lang/String;)V",
                                "org.springframework.context.event.EventListener")
                        .method("async", "(" + completed + ")V", "org.springframework.context.event.EventListener",
                                "org.springframework.scheduling.annotation.Async")
                        .methodWithParameterAnnotation("observed", "(" + completed + ")V", 0,
                                "jakarta.enterprise.event.Observes", "org.springframework.scheduling.annotation.Async"),
                GeneratedClass.named(WEB + "WebConfig")
                        .access(Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)
                        .annotatedWith("io.smallrye.config.ConfigMapping")
                        .method("port", "(I)V")
                        .abstractMethod("host", "()Ljava/lang/String;"),
                GeneratedClass.named(WEB + "Typed").annotatedWith("jakarta.enterprise.context.ApplicationScoped")
                        .implementing("Ljava/lang/Object;Ljava/lang/Comparable<Ljava/lang/String;>;"
                                + "Lio/quarkus/runtime/Supplier<Ljava/lang/String;>;",
                                "java.lang.Comparable", "io.quarkus.runtime.Supplier"),
                GeneratedClass.named(WEB + "Ghosts").annotatedWith("jakarta.enterprise.context.ApplicationScoped")
                        .field("ghosts", "Ljakarta/enterprise/event/Event;",
                                "Ljakarta/enterprise/event/Event<L" + (WEB + "Ghost").replace('.', '/') + ";>;",
                                "jakarta.inject.Inject"));
    }
}
