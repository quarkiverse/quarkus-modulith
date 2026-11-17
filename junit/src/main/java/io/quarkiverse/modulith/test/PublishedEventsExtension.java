package io.quarkiverse.modulith.test;

/**
 * Registers the resolution of {@link PublishedEvents} and {@link AssertablePublishedEvents} test method parameters in
 * a {@code @QuarkusTest} class that is not an {@link ApplicationModuleTest}:
 *
 * <pre>
 * &#64;QuarkusTest
 * &#64;ExtendWith(PublishedEventsExtension.class)
 * class OrderTest {
 *
 *     &#64;Test
 *     void publishesEvent(AssertablePublishedEvents events) {
 *         // …
 *     }
 * }
 * </pre>
 */
public final class PublishedEventsExtension extends PublishedEventsParameterResolver {

    /**
     * Called by JUnit.
     */
    public PublishedEventsExtension() {
        super();
    }
}
