package io.quarkiverse.modulith.test;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The member classes stand for a {@code @Nested} test class, which JUnit gives the extensions of the enclosing class,
 * and a static member class, which it does not.
 */
@ExtendWith({ PublishedEventsExtension.class, FixedWait.class })
class CustomizedFixture {

    class Inner {
    }

    static class Detached {
    }
}
