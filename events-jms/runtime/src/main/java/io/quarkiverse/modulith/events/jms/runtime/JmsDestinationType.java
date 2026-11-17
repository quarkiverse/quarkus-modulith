package io.quarkiverse.modulith.events.jms.runtime;

/**
 * The kind of JMS destination the routing target of an event names.
 */
public enum JmsDestinationType {

    /**
     * Point-to-point: the target is a queue name. The default, like Spring's {@code JmsTemplate}.
     */
    QUEUE,

    /**
     * Publish-subscribe: the target is a topic name.
     */
    TOPIC
}
