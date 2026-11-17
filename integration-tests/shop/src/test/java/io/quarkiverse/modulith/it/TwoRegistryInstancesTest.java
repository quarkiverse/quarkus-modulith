package io.quarkiverse.modulith.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.registry.DefaultEventPublicationRegistry;
import io.quarkiverse.modulith.events.spi.TransactionRunner;
import io.quarkiverse.modulith.it.notification.internal.AuditListener;
import io.quarkiverse.modulith.it.order.events.OrderAudited;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(NoStartupResubmissionProfile.class)
public class TwoRegistryInstancesTest {

    static final String LISTENER = "notification.audit";
    private static final long CLAIM_TIMEOUT_SECONDS = 60;

    @Inject
    EventPublicationRepository repository;

    @Inject
    TransactionRunner transactions;

    @Inject
    EventsConfig config;

    /**
     * The internal dispatcher on purpose: both registries must hand their claims to the dispatcher the application
     * really uses, and the test waits for its in-flight deliveries.
     */
    @Inject
    ModuleEventDispatcher dispatcher;

    private final Set<UUID> stored = new HashSet<>();

    @AfterEach
    void deleteThePublications() {
        AuditListener.DELIVERIES.clear();
        transactions.callInNewTransaction(() -> repository.deletePublicationsCounting(List.copyOf(stored)));
    }

    @Test
    void deliversEachPublicationOnceThroughTheRealDispatcher() {
        // arrange
        AuditListener.DELIVERIES.clear();
        for (int i = 0; i < 20; i++) {
            OrderAudited event = new OrderAudited("audit-" + i);
            stored.add(transactions.callInNewTransaction(() -> repository.create(TargetEventPublication.of(event,
                    PublicationTargetIdentifier.of(LISTENER), Instant.parse("2026-01-01T00:00:00Z"))).getIdentifier()));
        }
        DefaultEventPublicationRegistry one = registry();
        DefaultEventPublicationRegistry two = registry();

        // act
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> one.resubmitIncompletePublications(
                p -> p.getListenerId().equals(LISTENER)));
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> two.resubmitIncompletePublications(
                p -> p.getListenerId().equals(LISTENER)));
        int submitted = a.orTimeout(CLAIM_TIMEOUT_SECONDS, TimeUnit.SECONDS).join()
                + b.orTimeout(CLAIM_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(CLAIM_TIMEOUT_SECONDS));

        // assert
        assertThat(idle).isTrue();
        assertThat(submitted).isEqualTo(20);
        assertThat(AuditListener.DELIVERIES).hasSize(20)
                .allSatisfy((order, deliveries) -> assertThat(deliveries).hasValue(1));
        Map<UUID, Status> statuses = transactions.callInNewTransaction(() -> repository.findCompletedPublications())
                .stream().filter(p -> stored.contains(p.getIdentifier()))
                .collect(Collectors.toMap(TargetEventPublication::getIdentifier, TargetEventPublication::getStatus));
        assertThat(statuses).hasSize(20).containsOnlyKeys(stored).allSatisfy((id, status) -> assertThat(status)
                .isEqualTo(Status.COMPLETED));
    }

    DefaultEventPublicationRegistry registry() {
        return new DefaultEventPublicationRegistry(repository, transactions, config, Clock.systemUTC(), dispatcher);
    }

}
