package io.quarkiverse.modulith.events.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStoreProducer;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;

/**
 * Publications join the business transaction only with transaction management on, MongoDB with Panache and JTA
 * transactions. Extension tests always have both, so the step is called directly for the other combinations.
 */
class SessionSelectionTest {

    @ParameterizedTest
    @CsvSource({
            "true, io.quarkus.mongodb.panache, true, io.quarkiverse.modulith.events.mongodb.runtime.PanacheMongoSessions",
            "true, io.quarkus.mongodb.panache.kotlin, true, "
                    + "io.quarkiverse.modulith.events.mongodb.runtime.PanacheMongoSessions",
            "true, io.quarkus.mongodb.panache, false, io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions",
            "true, none, true, io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions",
            "false, io.quarkus.mongodb.panache, true, io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions" })
    void selectsTheSessionSource(boolean enabled, String panache, boolean transactions, Class<?> expected) {
        // arrange
        Capabilities capabilities = new Capabilities(Set.of(panache,
                transactions ? Capability.TRANSACTIONS : "nothing"));
        MongoDbBuildTimeConfig config = () -> () -> enabled;

        // act
        var beans = new EventsMongodbProcessor().beans(config, capabilities);

        // assert
        assertThat(beans.getBeanClasses()).containsExactly(MongoStoreProducer.class.getName(),
                MongoDbEventPublicationRepository.class.getName(), expected.getName());
    }
}
