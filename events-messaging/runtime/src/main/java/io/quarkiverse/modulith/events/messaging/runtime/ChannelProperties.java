package io.quarkiverse.modulith.events.messaging.runtime;

/**
 * Names the configuration properties of outgoing channels the way Quarkus Messaging reads them: a channel name with
 * dots, such as {@code order.OrderPlaced}, is quoted.
 */
public final class ChannelProperties {

    private static final String PREFIX = "mp.messaging.outgoing.";

    private ChannelProperties() {
    }

    /**
     * @return the property name of an attribute of an outgoing channel, such as
     *         {@code mp.messaging.outgoing."order.OrderPlaced".topic}
     */
    public static String outgoing(String channel, String attribute) {
        String name = channel.contains(".") ? "\"" + channel + "\"" : channel;
        return PREFIX + name + "." + attribute;
    }
}
