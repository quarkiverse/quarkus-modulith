package io.quarkiverse.modulith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.transaction.Transactional.TxType;

/**
 * Marks a method that reacts to an event published by another application module.
 * <p>
 * The method must declare exactly one parameter, the event. The event is fired with a plain CDI
 * {@code jakarta.enterprise.event.Event}. When the event is fired inside a transaction, the listener runs after that
 * transaction commits, and never if it rolls back. The listener runs on a worker thread, in a transaction of its own
 * when {@code quarkus-narayana-jta} is present.
 * <p>
 * With an event publication store such as {@code quarkus-modulith-events-jpa} every delivery is first stored in the
 * event publication table inside the publishing transaction, and marked completed when the listener succeeds.
 * Deliveries that fail, or that never ran because the application stopped, can be submitted again.
 * <p>
 * This is the equivalent of Spring Modulith's {@code @ApplicationModuleListener}. Like there, the annotation may be
 * used as a meta-annotation on a custom listener annotation, at any depth.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface ApplicationModuleListener {

    /**
     * A stable identifier for the listener. It is stored with every event publication, so keep it unchanged across
     * refactorings once publications are persisted. Defaults to {@code fully.qualified.Class.method(EventType)}.
     */
    String id() default "";

    /**
     * Whether the listener runs on a worker thread. When {@code false} it runs on the thread that completed the
     * publishing transaction, after the commit.
     */
    boolean async() default true;

    /**
     * A condition on the event. The listener only runs, and its publication is only stored, when it holds. Empty
     * means always. Spring Modulith evaluates SpEL; Quarkus Modulith compiles the same expressions at build time:
     * {@code #event} (or {@code event}, {@code #root.event}, {@code #root.args[0]}, {@code #a0}, {@code #p0}, the
     * parameter name), property paths and methods without parameters, literals, comparisons and logical operators.
     * Anything else fails the build.
     */
    String condition() default "";

    /**
     * Whether the listener's transaction is read-only. With a JPA store the Hibernate session is read-only and not
     * flushed.
     */
    boolean readOnlyTransaction() default false;

    /**
     * The transaction the listener runs in. {@code REQUIRES_NEW} and {@code REQUIRED} start a new one, since the
     * publishing transaction has completed; {@code SUPPORTS}, {@code NOT_SUPPORTED} and {@code NEVER} run without one.
     * {@code MANDATORY} fails the build.
     */
    TxType propagation() default TxType.REQUIRES_NEW;
}
