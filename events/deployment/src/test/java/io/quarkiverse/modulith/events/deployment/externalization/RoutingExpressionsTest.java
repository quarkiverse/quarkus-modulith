package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.AuditLog;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.AuditRecord;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Counter;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Customer;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Grade;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Login;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Logout;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.NullTarget;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Nullable;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Ordered;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Reported;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.SimpleLabel;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Tagged;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Tags;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit.Untagged;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.Repositories;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkiverse.modulith.events.externalization.runtime.RoutingResolver;
import io.quarkus.test.QuarkusExtensionTest;

class RoutingExpressionsTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, Login.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class, Repositories.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    AuditLog log;

    @Inject
    RecordingTransport transport;

    @Inject
    EventPublicationRepository repository;

    @Inject
    RoutingResolver resolver;

    @BeforeEach
    void reset() {
        transport.reset();
    }

    private static Object event(String name) {
        return switch (name) {
            case "login" -> new Login("bob", 3);
            case "logout" -> new Logout("bob");
            case "grade" -> new Grade('A', true, 42L, (byte) 7, (short) 9, 2.5, 0.5f, null);
            case "other-grade" -> new Grade('B', false, 1L, (byte) 0, (short) 0, 0.0, 1.0f, "n");
            case "nullable" -> new Nullable("k-1");
            case "null-key" -> new Nullable(null);
            case "tagged" -> new Tagged(new SimpleLabel("red"));
            case "counter" -> new Counter(5);
            case "record" -> new AuditRecord("a-1");
            default -> new Reported("weekly");
        };
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "login|logins::bob",
            "logout|bob-out::k-Logout",
            "grade|grades::A-true-42-7-9-2.5-0.5-",
            "other-grade|grades::B-false-1-0-0-0.0-1.0-n",
            "nullable|nullables::k-1",
            "null-key|nullables",
            "tagged|red::tag-red",
            "counter|counters::5",
            "record|auditables::a-1",
            "reported|meta::weekly"
    })
    void resolvesCompiledExpressions(String name, String expected) {
        // act
        log.publish(event(name));

        // assert
        await().atMost(TIMEOUT).until(() -> transport.sent().size() == 1);
        assertThat(transport.sent().get(0).target()).isEqualTo(RoutingTarget.parse(expected));
        assertThat(transport.sent().get(0).target()).hasToString(expected);
    }

    @Test
    void destroysDependentBeansAfterTheCall() {
        // arrange
        int before = Tags.DESTROYED.get();

        // act
        log.publish(new Tagged(new SimpleLabel("blue")));

        // assert
        await().atMost(TIMEOUT).until(() -> transport.sent().size() == 1);
        assertThat(Tags.DESTROYED.get()).isEqualTo(before + 1);
    }

    @Test
    void resolvesAChainThroughAPresentValue() {
        // act
        RoutingTarget target = resolver.resolve(RoutingTarget.parse("orders::{event.customer.id}"),
                new Ordered(new Customer("c-1")));

        // assert
        assertThat(target).hasToString("orders::c-1");
    }

    @Test
    void namesTheExpressionWhenAValueInTheChainIsNull() {
        // act
        Throwable thrown = catchThrowable(
                () -> resolver.resolve(RoutingTarget.parse("orders::{event.customer.id}"), new Ordered(null)));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Routing expression '{event.customer.id}' for " + Ordered.class.getName()
                        + " cannot be evaluated: event.customer is null");
    }

    @Test
    void destroysDependentBeansWhenTheCallFails() {
        // arrange
        int before = Tags.DESTROYED.get();

        // act
        Throwable thrown = catchThrowable(() -> resolver.resolve(
                RoutingTarget.parse("untagged::#{@tags.refuse(#this)}"), new Untagged("plain")));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("no tag for plain");
        assertThat(Tags.DESTROYED.get()).isEqualTo(before + 1);
    }

    @Test
    void failsWhenTheTargetEvaluatesToNull() {
        // act
        log.publish(new NullTarget(null));

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).stream()
                .anyMatch(publication -> publication.getEvent().equals(new NullTarget(null))));
        assertThat(transport.sent()).isEmpty();
    }
}
