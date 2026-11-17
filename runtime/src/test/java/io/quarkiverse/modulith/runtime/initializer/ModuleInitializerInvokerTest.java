package io.quarkiverse.modulith.runtime.initializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.runtime.cdi.FakeBean;
import io.quarkiverse.modulith.runtime.cdi.FakeHandle;
import io.quarkiverse.modulith.runtime.cdi.FakeInstance;

class ModuleInitializerInvokerTest {

    private final List<String> calls = new ArrayList<>();

    @Test
    void runsInitializersInModuleOrderAndDestroysDependentOnes() {
        // arrange
        ModuleInitializerInvoker invoker = new ModuleInitializerInvoker();
        invoker.order = new InitializerOrder(
                List.of(InventoryInitializer.class.getName(), OrdersInitializer.class.getName()));
        invoker.initializers = new FakeInstance<>(List.of(
                handle(OrdersInitializer.class, Singleton.class, new OrdersInitializer(calls, null)),
                handle(InventoryInitializer.class, Dependent.class, new InventoryInitializer(calls))));

        // act
        invoker.onStart(null);

        // assert
        assertThat(calls).containsExactly("initialize inventory", "destroy InventoryInitializer",
                "initialize orders");
    }

    @Test
    void destroysADependentInitializerThatFails() {
        // arrange
        IllegalStateException failure = new IllegalStateException("orders down");
        Instance.Handle<ApplicationModuleInitializer> handle = handle(OrdersInitializer.class, Dependent.class,
                new OrdersInitializer(calls, failure));

        // act & assert
        assertThatThrownBy(() -> ModuleInitializerInvoker.invoke(handle)).isSameAs(failure);
        assertThat(calls).containsExactly("initialize orders", "destroy OrdersInitializer");
    }

    private Instance.Handle<ApplicationModuleInitializer> handle(Class<?> beanClass,
            Class<? extends Annotation> scope, ApplicationModuleInitializer initializer) {
        return new FakeHandle<>(new FakeBean<>(beanClass, scope), initializer, calls);
    }
}
