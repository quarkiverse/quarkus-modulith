package io.quarkiverse.modulith.docs;

/**
 * The bean groups Quarkus Modulith uses by default, named after the Quarkus concepts: REST resources, Services,
 * Repositories, Event listeners, Configuration mappings, Scheduled jobs and Messaging endpoints.
 * <p>
 * The Quarkus build reads the scheduled jobs ({@code @Scheduled}) and messaging endpoints ({@code @Incoming},
 * {@code @Outgoing}) from the method annotations in the Jandex index. A {@link Documenter} in a plain test works from
 * the core module model, which does not carry them, so its canvases show those beans in their other groups or under
 * "Others".
 */
public final class QuarkusGroupings {

    private QuarkusGroupings() {
    }

    public static Grouping[] getGroupings() {
        return new Grouping[] {
                Grouping.of("REST resources", bean -> bean.is(Stereotype.CONTROLLER)),
                Grouping.of("Services", bean -> bean.is(Stereotype.SERVICE)),
                Grouping.of("Repositories", bean -> bean.is(Stereotype.REPOSITORY)),
                Grouping.of("Event listeners", bean -> bean.is(Stereotype.EVENT_LISTENER)),
                Grouping.of("Configuration mappings", bean -> bean.is(Stereotype.CONFIGURATION_PROPERTIES)),
                Grouping.of("Scheduled jobs", bean -> bean.is(Stereotype.SCHEDULED_JOB)),
                Grouping.of("Messaging endpoints", bean -> bean.is(Stereotype.MESSAGING_ENDPOINT))
        };
    }
}
