package io.quarkiverse.modulith.events.messaging.runtime;

import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;

/**
 * The Quarkus Messaging transport as a default bean, so that a transport bean of the application replaces it.
 */
@Singleton
@DefaultBean
public class DefaultMessagingTransport extends MessagingExternalizationTransport {
}
