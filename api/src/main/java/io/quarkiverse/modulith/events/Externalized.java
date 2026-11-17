package io.quarkiverse.modulith.events;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an event type to be published to a message broker, in addition to the module listeners, like Spring
 * Modulith's {@code @Externalized}. jMolecules' {@code org.jmolecules.event.annotation.Externalized} is honored the same
 * way. The annotation is found on the event type, its superclasses and interfaces, and on annotations of those.
 * <p>
 * The routing target is written {@code target} or {@code target::key}. The broker decides what a target is: a Kafka
 * topic, a RabbitMQ exchange, a JMS destination. Either part may be an expression evaluated against the event, which
 * the build compiles and validates: <code>customer-created::{event.lastname}</code>,
 * <code>{inject:keys.of(event)}</code>, or Spring's {@code #{#this.getLastname()}} and {@code #{@keys.of(#this)}}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface Externalized {

    /**
     * The routing target, written {@code target} or {@code target::key}. Defaults to the application-local name of the
     * type, for example {@code order.OrderPlaced} for {@code com.acme.order.OrderPlaced} in an application with base
     * package {@code com.acme}. Alias of {@link #target()}.
     */
    String value() default "";

    /**
     * Alias of {@link #value()}. When both are set, {@code target} wins.
     */
    String target() default "";
}
