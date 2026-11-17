package io.quarkiverse.modulith.events.jms.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.jms.ConnectionFactory;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationBuildItem;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationTransportBuildItem;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.jms.runtime.JmsTransportProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Build steps of {@code quarkus-modulith-events-jms}: announces the JMS transport to the externalization core and
 * registers it when externalization is active.
 */
class EventsJmsProcessor {

    static final String FEATURE = "modulith-events-jms";
    private static final DotName PRODUCER = DotName.createSimple(JmsTransportProducer.class);
    private static final DotName TRANSPORT = DotName.createSimple(EventExternalizationTransport.class);
    private static final DotName CONNECTION_FACTORY = DotName.createSimple(ConnectionFactory.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExternalizationTransportBuildItem transport() {
        return new ExternalizationTransportBuildItem("jms");
    }

    @BuildStep
    void beans(ExternalizationBuildItem externalization, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (externalization.isActive()) {
            beans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(JmsTransportProducer.class).build());
        }
    }

    /**
     * The JMS transport looks up the {@code @Default} connection factory, so the build fails when there is none or
     * when several are ambiguous, unless the application provides a transport of its own.
     */
    @BuildStep
    void validate(ValidationPhaseBuildItem validation, BuildProducer<ValidationErrorBuildItem> errors) {
        Set<DotName> transports = validation.getContext().beans().withBeanType(TRANSPORT).stream()
                .map(BeanInfo::getBeanClass).collect(Collectors.toSet());
        if (!transports.equals(Set.of(PRODUCER))) {
            return;
        }
        BeanResolver resolver = validation.getContext().get(BuildExtension.Key.DEPLOYMENT).getBeanResolver();
        Set<BeanInfo> factories = resolver.resolveBeans(Type.create(CONNECTION_FACTORY, Type.Kind.CLASS));
        if (factories.isEmpty()) {
            errors.produce(new ValidationErrorBuildItem(new IllegalStateException("Externalizing events to JMS needs a"
                    + " jakarta.jms.ConnectionFactory bean: add a JMS client extension such as"
                    + " io.quarkiverse.artemis:quarkus-artemis-jms or io.quarkiverse.qpid:quarkus-qpid-jms, or set"
                    + " quarkus.modulith.events.externalization.enabled=false")));
            return;
        }
        try {
            resolver.resolveAmbiguity(factories);
        } catch (AmbiguousResolutionException e) {
            errors.produce(new ValidationErrorBuildItem(new IllegalStateException("Externalizing events to JMS needs"
                    + " one @Default jakarta.jms.ConnectionFactory bean, but found " + factories.stream()
                            .map(BeanInfo::getBeanClass).map(DotName::toString).sorted().toList()
                    + ": give all but one of them a qualifier, or mark the one to use"
                    + " @Alternative @Priority(1)", e)));
        }
    }
}
