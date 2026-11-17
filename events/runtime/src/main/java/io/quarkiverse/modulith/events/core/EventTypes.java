package io.quarkiverse.modulith.events.core;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

/**
 * Loads the class of a stored event, for every store. The events deployment module registers every listened type and
 * its known subtypes for reflection, so the classes stored by this application load in native executables too.
 */
public final class EventTypes {

    private static final Logger LOG = Logger.getLogger(EventTypes.class);
    static final Set<String> REPORTED_MISSING = ConcurrentHashMap.newKeySet();

    private EventTypes() {
    }

    /**
     * Loads through the thread context class loader, or this class's loader on threads without one.
     *
     * @param publication the publication the type belongs to, for the messages
     * @param eventType the fully qualified class name stored with the publication
     * @throws IllegalStateException if the class does not exist anymore, after logging a warning that names the
     *         publication and the type, once per type
     */
    public static Class<?> load(UUID publication, String eventType) {
        ClassLoader loader = Objects.requireNonNullElse(Thread.currentThread().getContextClassLoader(),
                EventTypes.class.getClassLoader());
        try {
            return Class.forName(eventType, false, loader);
        } catch (ClassNotFoundException e) {
            if (REPORTED_MISSING.add(eventType)) {
                LOG.warnf("Event type %s of publication %s is not on the class path, so its publications cannot be"
                        + " read; delete them or restore the type", eventType, publication);
            }
            throw new IllegalStateException("Cannot read event publication " + publication + " of type " + eventType, e);
        }
    }
}
