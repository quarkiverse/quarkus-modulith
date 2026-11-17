package io.quarkiverse.modulith.events.jpa.deployment.library;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Named;

/**
 * Packaged as a separate bean archive, the way a library observer reaches the application. The test archive loads its
 * classes in a class loader of its own, so the observer methods are public (the generated observers live in the
 * application's loader), and the test talks to it through JDK types only: it fires a {@code String} named
 * {@value #TRIGGER}, the library publishes its own {@link LibraryEvent}, and the calls are kept in a system property.
 */
@ApplicationScoped
public class LibraryObserver {

    public static final String TRIGGER = "library-trigger";
    public static final String CALLS = "modulith.test.library-observer";

    public void publish(@Observes @Named(TRIGGER) String id, Event<LibraryEvent> events) {
        events.fire(new LibraryEvent(id));
    }

    public void afterSuccess(@Observes(during = TransactionPhase.AFTER_SUCCESS) LibraryEvent event) {
        System.setProperty(CALLS, System.getProperty(CALLS, "") + "library:" + event.id() + ";");
    }
}
