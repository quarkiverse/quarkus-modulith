package io.quarkiverse.modulith.events.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional.TxType;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.deployment.ModuleListenersBuildItem;
import io.quarkiverse.modulith.events.deployment.fixtures.Address;
import io.quarkiverse.modulith.events.deployment.fixtures.ExpressParcel;
import io.quarkiverse.modulith.events.deployment.fixtures.LibraryTicket;
import io.quarkiverse.modulith.events.deployment.fixtures.Parcel;
import io.quarkiverse.modulith.events.deployment.fixtures.PriorityParcel;
import io.quarkiverse.modulith.events.deployment.fixtures.Shipment;
import io.quarkiverse.modulith.events.deployment.fixtures.Ticket;
import io.quarkiverse.modulith.runtime.events.ListenerData;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.smallrye.config.SmallRyeConfigBuilder;

class EventsProcessorTest {

    @Test
    void registersListenedTypesWithEveryKnownSubtypeOnce() throws IOException {
        // arrange
        Index index = Index.of(Shipment.class, Parcel.class, Address.class, ExpressParcel.class, PriorityParcel.class);
        ListenerData byInterface = listener("by-interface", Shipment.class.getName());
        ListenerData byClass = listener("by-class", ExpressParcel.class.getName());
        ModuleListenersBuildItem listeners = new ModuleListenersBuildItem(List.of(byInterface, byClass, byInterface));

        // act
        var types = EventsProcessor.serializedTypes(listeners, index, name -> true);

        // assert
        assertThat(types).containsExactlyInAnyOrder(DotName.createSimple(Shipment.class),
                DotName.createSimple(ExpressParcel.class),
                DotName.createSimple(PriorityParcel.class), DotName.createSimple(Parcel.class));
    }

    @Test
    void registersOnlyApplicationSubtypesOfJdkTypes() throws IOException {
        // arrange
        Index index = Index.of(Ticket.class, LibraryTicket.class, Parcel.class);
        ModuleListenersBuildItem listeners = new ModuleListenersBuildItem(
                List.of(listener("everything", Serializable.class.getName())));

        // act
        var types = EventsProcessor.serializedTypes(listeners, index, Ticket.class.getName()::equals);

        // assert
        assertThat(types).containsExactly(DotName.createSimple(Ticket.class));
    }

    @Test
    void registersNothingWithoutListeners() throws IOException {
        // arrange
        Index index = Index.of(Shipment.class, Parcel.class);

        // act
        var types = EventsProcessor.serializedTypes(new ModuleListenersBuildItem(List.of()), index, name -> true);

        // assert
        assertThat(types).isEmpty();
    }

    @Test
    void registersTheSerializedTypesWithTheirHierarchyForNative() throws IOException {
        // arrange
        Index index = Index.of(Shipment.class, Parcel.class, Address.class);
        List<ReflectiveHierarchyBuildItem> produced = new ArrayList<>();
        ModuleListenersBuildItem listeners = new ModuleListenersBuildItem(List.of(listener("l", Parcel.class.getName())));

        // act
        new EventsProcessor().registerEventTypesForReflection(listeners, new CombinedIndexBuildItem(index, index),
                produced::add);

        // assert
        assertThat(produced).singleElement().satisfies(item -> {
            assertThat(item.getType().name()).isEqualTo(DotName.createSimple(Parcel.class));
            assertThat(item.getSource()).isEqualTo("Module events are serialized for the event publication registry");
        });
    }

    @ParameterizedTest
    @CsvSource(value = { "quarkus.modulith.events.republish-interval,1s,true",
            "quarkus.modulith.events.staleness.processing,5m,true",
            "quarkus.modulith.events.republish-interval,'',false",
            "quarkus.modulith.events.republish-older-than,1m,false" })
    void seesAJobOnlyWhenAnIntervalOrThresholdHasAValue(String key, String value, boolean expected) {
        // arrange
        var config = new SmallRyeConfigBuilder().withDefaultValue(key, value).build();

        // act
        boolean configured = EventsProcessor.jobConfigured(config);

        // assert
        assertThat(configured).isEqualTo(expected);
    }

    static ListenerData listener(String id, String eventType) {
        return new ListenerData(id, "C", "on", eventType, "order", true, "Invoker", eventType, "", TxType.REQUIRES_NEW,
                false);
    }
}
