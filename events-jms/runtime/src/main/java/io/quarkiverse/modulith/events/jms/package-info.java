/**
 * Externalizes module events through a Jakarta Messaging {@code JMSContext}, like Spring Modulith's
 * {@code spring-modulith-events-jms}. The transport lives in the {@code runtime} package and needs a
 * {@code jakarta.jms.ConnectionFactory} bean, for example from the Quarkiverse Artemis JMS or Qpid JMS extension.
 */
package io.quarkiverse.modulith.events.jms;
