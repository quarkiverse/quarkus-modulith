package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.test.slice.billing.ListenerOnly;
import io.quarkiverse.modulith.test.slice.billing.ObserverOnly;
import io.quarkiverse.modulith.test.slice.billing.ProducerOnly;
import io.quarkiverse.modulith.test.slice.billing.ScopedBean;
import io.quarkiverse.modulith.test.slice.billing.internal.BillingSliceFixture;
import io.quarkiverse.modulith.test.slice.billing.internal.InternalBean;
import io.quarkus.arc.Arc;
import io.quarkus.test.component.QuarkusComponentTestExtension;
import io.quarkus.test.component.SkipInject;

/**
 * Starts the billing module of the {@code slice} fixture application in a component test, to check the scopes the
 * slice gives its beans. The injected fields keep ArC from removing the beans as unused.
 */
class ApplicationModuleSliceTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = ApplicationModuleComponents
            .of(BillingSliceFixture.class).build();

    @Inject
    ListenerOnly listener;

    @Inject
    ProducerOnly producer;

    @Inject
    ScopedBean scoped;

    @Inject
    InternalBean internal;

    static Stream<Arguments> components() {
        return Stream.of(Arguments.of(ListenerOnly.class, Singleton.class),
                Arguments.of(ProducerOnly.class, Dependent.class),
                Arguments.of(ObserverOnly.class, Dependent.class),
                Arguments.of(ScopedBean.class, ApplicationScoped.class),
                Arguments.of(InternalBean.class, Singleton.class));
    }

    @ParameterizedTest
    @MethodSource("components")
    void givesEachComponentTheScopeOfTheApplication(@SkipInject Class<?> component,
            @SkipInject Class<? extends Annotation> scope) {
        // act
        Class<? extends Annotation> result = Arc.container().instance(component).getBean().getScope();

        // assert
        assertThat(result).isEqualTo(scope);
    }
}
