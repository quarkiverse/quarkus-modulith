package io.quarkiverse.modulith.events.messaging.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time settings of event externalization through Quarkus Messaging. Quarkus Messaging channels are static, so
 * the build generates one outgoing channel per routing target it knows: the static targets of {@code Externalized}
 * events, the channels listed here and the default channel. Configure each channel with the usual
 * {@code mp.messaging.outgoing.<channel>.*} properties; connectors default the topic, exchange or address to the
 * channel name.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.messaging")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface MessagingBuildTimeConfig {

    /**
     * Additional routing targets that get a channel of their own, for targets a custom
     * {@code EventExternalizationConfiguration} routes to. A channel is named like its target.
     */
    Optional<List<String>> channels();

    /**
     * The channel of events whose target has no channel of their own, such as targets computed by expressions. The
     * connector then addresses the target per message: the Kafka topic, the AMQP 1.0 address and, on Quarkus 4, the
     * RabbitMQ exchange. Without a default channel such events fail to externalize and their publications stay
     * incomplete.
     */
    Optional<String> defaultChannel();

    /**
     * Whether events are sent as JSON written by the {@code EventSerializer}, with the content type
     * {@code application/json} for RabbitMQ and AMQP 1.0, and Kafka's {@code StringSerializer} as the default value
     * serializer of the generated channels. Otherwise events are sent as they are and the connector's serializer writes
     * them. Like Spring Modulith's {@code spring.modulith.events.kafka.enable-json} and
     * {@code spring.modulith.events.rabbitmq.enable-json}.
     */
    @WithDefault("true")
    boolean enableJson();
}
