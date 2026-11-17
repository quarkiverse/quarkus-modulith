package io.quarkiverse.modulith.events.messaging.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

/**
 * The Quarkus Messaging transport as a reserve bean, so that a transport bean of the application replaces it. Used
 * with Quarkus 4.
 */
@Singleton
@Reserve
@Priority(0)
public class DefaultMessagingTransport extends MessagingExternalizationTransport {
}
