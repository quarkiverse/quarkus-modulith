package io.quarkiverse.modulith.events.messaging.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.quarkiverse.modulith.events.ExternalizationMode;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationBuildItem;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationTransportBuildItem;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizedTypeBuildItem;
import io.quarkiverse.modulith.events.messaging.impl.MessagingChannels;
import io.quarkiverse.modulith.events.messaging.runtime.AmqpConnectorMetadata;
import io.quarkiverse.modulith.events.messaging.runtime.ChannelProperties;
import io.quarkiverse.modulith.events.messaging.runtime.DefaultMessagingTransport;
import io.quarkiverse.modulith.events.messaging.runtime.JsonPayloads;
import io.quarkiverse.modulith.events.messaging.runtime.KafkaConnectorMetadata;
import io.quarkiverse.modulith.events.messaging.runtime.PlainPayloads;
import io.quarkiverse.modulith.events.messaging.runtime.RabbitConfirmsCheck;
import io.quarkiverse.modulith.events.messaging.runtime.RabbitConfirmsWarning;
import io.quarkiverse.modulith.events.messaging.runtime.RabbitConnectorMetadata;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

/**
 * Build steps of {@code quarkus-modulith-events-messaging}: provides the Quarkus Messaging externalization transport,
 * generates one outgoing channel per known routing target and registers the metadata of the connectors present.
 */
class EventsMessagingProcessor {

    static final String FEATURE = "modulith-events-messaging";
    static final String GENERATED_CHANNELS = "io.quarkiverse.modulith.events.messaging.impl.GeneratedMessagingChannels";
    static final String STRING_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    static final String KAFKA_CONNECTOR = "smallrye-kafka";
    static final String RABBITMQ_CONNECTOR = "smallrye-rabbitmq";
    private static final String CONNECTOR = "connector";
    private static final String EMITTER_SIGNATURE = "Lorg/eclipse/microprofile/reactive/messaging/Emitter"
            + "<Ljava/lang/Object;>;";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExternalizationTransportBuildItem transport() {
        return new ExternalizationTransportBuildItem("messaging");
    }

    @BuildStep
    MessagingConnectorsBuildItem connectors(CurateOutcomeBuildItem curateOutcome) {
        return MessagingConnectorsBuildItem.of(curateOutcome.getApplicationModel().getRuntimeDependencies());
    }

    @BuildStep
    MessagingChannelsBuildItem channels(ExternalizationBuildItem externalization,
            List<ExternalizedTypeBuildItem> types, MessagingBuildTimeConfig config) {
        Set<String> names = new TreeSet<>();
        if (externalization.isActive()) {
            types.forEach(type -> type.getStaticTarget().ifPresent(names::add));
            config.channels().ifPresent(names::addAll);
            config.defaultChannel().ifPresent(names::add);
        }
        return new MessagingChannelsBuildItem(names, config.defaultChannel());
    }

    @BuildStep
    void beans(ExternalizationBuildItem externalization, MessagingConnectorsBuildItem connectors,
            MessagingBuildTimeConfig config, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (!externalization.isActive()) {
            return;
        }
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultMessagingTransport.class,
                        config.enableJson() ? JsonPayloads.class : PlainPayloads.class);
        if (connectors.hasKafka()) {
            builder.addBeanClasses(KafkaConnectorMetadata.class);
        }
        if (connectors.hasRabbitmq()) {
            builder.addBeanClasses(RabbitConnectorMetadata.class);
        }
        if (connectors.hasAmqp()) {
            builder.addBeanClasses(AmqpConnectorMetadata.class);
        }
        if (connectors.hasRabbitmq()) {
            builder.addBeanClasses(externalization.getMode() == ExternalizationMode.OUTBOX ? RabbitConfirmsCheck.class
                    : RabbitConfirmsWarning.class);
        }
        beans.produce(builder.build());
    }

    /**
     * Generates the subclass of {@link MessagingChannels} with one {@code @Channel} emitter field per channel, so that
     * Quarkus Messaging declares and wires the channels. Application scoped, so that the emitters are only injected
     * when the first event is sent: the externalization beans may be created during static initialization, before
     * Quarkus Messaging has wired its channels.
     */
    @BuildStep
    void generateChannels(ExternalizationBuildItem externalization, MessagingChannelsBuildItem channels,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        if (!externalization.isActive()) {
            return;
        }
        try (ClassCreator creator = ClassCreator.builder().classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                .className(GENERATED_CHANNELS).superClass(MessagingChannels.class).build()) {
            creator.addAnnotation(ApplicationScoped.class);
            MethodCreator constructor = creator.getMethodCreator(MethodDescriptor.ofConstructor(GENERATED_CHANNELS));
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(MessagingChannels.class, String.class),
                    constructor.getThis(), channels.getDefaultChannel().map(constructor::load)
                            .orElseGet(constructor::loadNull));
            constructor.returnVoid();
            MethodCreator register = creator.getMethodCreator("registerChannels", void.class);
            register.addAnnotation(PostConstruct.class);
            MethodDescriptor registration = MethodDescriptor.ofMethod(MessagingChannels.class, "register", void.class,
                    String.class, Emitter.class);
            int index = 0;
            for (String channel : channels.getNames()) {
                FieldCreator field = creator.getFieldCreator("channel" + index++, Emitter.class).setModifiers(0);
                field.setSignature(EMITTER_SIGNATURE);
                field.addAnnotation(Inject.class);
                field.addAnnotation(Channel.class).addValue("value", channel);
                register.invokeVirtualMethod(registration, register.getThis(), register.load(channel),
                        register.readInstanceField(field.getFieldDescriptor(), register.getThis()));
            }
            register.returnVoid();
        }
    }

    /**
     * Sets defaults on the generated channels so that a message counts as sent only once the broker stored it: Kafka
     * waits for every in-sync replica ({@code acks=all}) with idempotent retries and serializes JSON payloads with
     * {@code StringSerializer}, RabbitMQ uses publisher confirms. The defaults are channel properties, so they take
     * precedence over connector-wide settings and are overridden per channel only; {@code acks} and
     * {@code enable.idempotence} are overridden together, since an idempotent producer needs {@code acks=all}.
     * When the build knows the connector of a channel ({@code mp.messaging.outgoing.<channel>.connector}), only the
     * defaults of that connector apply.
     */
    @BuildStep
    void channelDefaults(MessagingChannelsBuildItem channels, MessagingConnectorsBuildItem connectors,
            MessagingBuildTimeConfig config, BuildProducer<RunTimeConfigurationDefaultBuildItem> defaults) {
        Config build = ConfigProvider.getConfig();
        channelDefaults(channels.getNames(), connectors, config.enableJson(),
                channel -> build.getOptionalValue(ChannelProperties.outgoing(channel, CONNECTOR), String.class))
                .forEach(defaults::produce);
    }

    static List<RunTimeConfigurationDefaultBuildItem> channelDefaults(Set<String> channels,
            MessagingConnectorsBuildItem connectors, boolean json, Function<String, Optional<String>> connectorOf) {
        List<RunTimeConfigurationDefaultBuildItem> defaults = new ArrayList<>();
        for (String channel : channels) {
            Optional<String> connector = connectorOf.apply(channel);
            if (connectors.hasKafka() && connector.map(KAFKA_CONNECTOR::equals).orElse(true)) {
                defaults.add(new RunTimeConfigurationDefaultBuildItem(ChannelProperties.outgoing(channel, "acks"),
                        "all"));
                defaults.add(new RunTimeConfigurationDefaultBuildItem(
                        ChannelProperties.outgoing(channel, "enable.idempotence"), "true"));
                if (json) {
                    defaults.add(new RunTimeConfigurationDefaultBuildItem(
                            ChannelProperties.outgoing(channel, "value.serializer"), STRING_SERIALIZER));
                }
            }
            if (connectors.hasRabbitmq() && connector.map(RABBITMQ_CONNECTOR::equals).orElse(true)) {
                defaults.add(new RunTimeConfigurationDefaultBuildItem(
                        ChannelProperties.outgoing(channel, "publish-confirms"), "true"));
            }
        }
        return defaults;
    }
}
