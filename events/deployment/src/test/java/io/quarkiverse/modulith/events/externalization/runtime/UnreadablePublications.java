package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.List;

import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * Reads the publications of the event {@code "poison"} as publications whose event type is gone.
 */
class UnreadablePublications extends ExternalizationPublications {

    @Override
    public List<TargetEventPublication> findPending(String listenerId, PageCriteria page) {
        return super.findPending(listenerId, page).stream()
                .map(publication -> "poison".equals(publication.getEvent()) ? new UnreadablePublication(publication)
                        : publication)
                .toList();
    }
}
