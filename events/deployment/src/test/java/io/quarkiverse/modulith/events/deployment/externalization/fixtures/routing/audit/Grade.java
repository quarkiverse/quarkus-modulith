package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("grades::{event.grade}-{event.passed}-{event.at}-{event.level}-{event.shard}-{event.score}-{event.ratio}-{event.note}")
public record Grade(char grade, boolean passed, long at, byte level, short shard, double score, float ratio,
        String note) {
}
