package io.quarkiverse.modulith.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.jdbc.SpringModulithWriter;

import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.it.order.events.OrderPlaced;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpringModulithMigrationTest {

    @Inject
    DataSource dataSource;

    @Inject
    IncompleteEventPublications incomplete;

    @Test
    void readsAndResubmitsAPublicationWrittenBySpringModulith() {
        // arrange
        OrderPlaced event = new OrderPlaced("spring-1", "sku-1", 1);
        List<UUID> written = SpringModulithWriter.store(dataSource, event, List.of("notification.order-confirmation"));

        // act
        List<EventPublication> read = incomplete.findIncompletePublications().stream()
                .filter(p -> p.getIdentifier().equals(written.get(0))).toList();
        int submitted = incomplete.resubmitIncompletePublications(p -> p.getIdentifier().equals(written.get(0)));

        // assert
        assertThat(read).singleElement().satisfies(p -> {
            assertThat(p.getListenerId()).isEqualTo("notification.order-confirmation");
            assertThat(p.getEventType()).isEqualTo(OrderPlaced.class.getName());
            assertThat(p.getEvent()).isEqualTo(event);
            assertThat(p.getStatus()).isEqualTo(Status.PUBLISHED);
            assertThat(p.getCompletionAttempts()).as("Spring Modulith 2.1.2 stores new publications with one attempt")
                    .isEqualTo(1);
        });
        assertThat(submitted).isEqualTo(1);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            given().get("/notifications/spring-1").then().body(equalTo("true"));
            assertThat(SpringModulithWriter.describe(dataSource))
                    .contains("notification.order-confirmation=COMPLETED");
        });
    }
}
