package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * Helpers the contract tests share.
 */
public final class Publications {

    private Publications() {
    }

    /**
     * @return the only publication, failing the test if there is not exactly one
     */
    public static TargetEventPublication single(List<TargetEventPublication> publications) {
        assertThat(publications).hasSize(1);
        return publications.get(0);
    }

    /**
     * @return the identifiers of the {@link TckEvent}s of the publications, in order
     */
    public static List<String> events(List<TargetEventPublication> publications) {
        return publications.stream().map(p -> ((TckEvent) p.getEvent()).id()).toList();
    }
}
