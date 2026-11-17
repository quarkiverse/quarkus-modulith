package io.quarkiverse.modulith.docs;

/**
 * The bean groups Spring Modulith uses by default, with Spring's names: Controllers, Services, Repositories, Event
 * listeners and Configuration properties. Use them to produce canvases that read like Spring Modulith's.
 */
public final class SpringGroupings {

    private SpringGroupings() {
    }

    public static Grouping[] getGroupings() {
        return new Grouping[] {
                Grouping.of("Controllers", bean -> bean.is(Stereotype.CONTROLLER)),
                Grouping.of("Services", bean -> bean.is(Stereotype.SERVICE)),
                Grouping.of("Repositories", bean -> bean.is(Stereotype.REPOSITORY)),
                Grouping.of("Event listeners", bean -> bean.is(Stereotype.EVENT_LISTENER)),
                Grouping.of("Configuration properties", bean -> bean.is(Stereotype.CONFIGURATION_PROPERTIES))
        };
    }
}
