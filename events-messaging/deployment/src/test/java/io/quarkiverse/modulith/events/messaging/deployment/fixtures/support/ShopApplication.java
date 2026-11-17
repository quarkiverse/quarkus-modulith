package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.messaging.runtime.ChannelProperties;
import io.quarkus.test.QuarkusExtensionTest;

public final class ShopApplication {

    public static final String BASE_PACKAGE = "io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop";

    private ShopApplication() {
    }

    public static QuarkusExtensionTest create(Class<?>... classes) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                        .addClasses(Repositories.class, ExternalizedEvents.class, RecordingConnector.class,
                                RecordingSubscriber.class, RecordingBroker.class, BrokerMode.class, ShopApplication.class)
                        .addClasses(classes))
                .overrideConfigKey("quarkus.modulith.base-package", BASE_PACKAGE)
                .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
                .overrideConfigKey("quarkus.devservices.enabled", "false");
    }

    public static QuarkusExtensionTest recording(QuarkusExtensionTest test, String... channels) {
        for (String channel : channels) {
            test.overrideConfigKey(ChannelProperties.outgoing(channel, "connector"), RecordingConnector.NAME);
        }
        return test;
    }

    public static QuarkusExtensionTest recordingStaticTargets(QuarkusExtensionTest test) {
        return recording(test, "orders", "shipments", "order.OrderCancelled", "order.OrderAudited");
    }
}
