package io.quarkiverse.modulith.it.moduletests.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.it.moduletests.inventory.Inventory;
import io.quarkiverse.modulith.it.moduletests.inventory.StockReduced;
import io.quarkiverse.modulith.test.ApplicationModuleComponents;
import io.quarkus.arc.Arc;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTestExtension;
import io.quarkus.test.component.SkipInject;

class ShippingComponentTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = ApplicationModuleComponents
            .of(ShippingComponentTest.class).build();

    @Inject
    Shipping shipping;

    @Inject
    ShippingPolicy policy;

    @Inject
    Parcels parcels;

    @Inject
    ShippingListener listener;

    @InjectMock
    Inventory inventory;

    @ParameterizedTest
    @CsvSource({ "5, 3, true", "5, 6, false", "50, 11, false" })
    void shipsWithinStockAndPolicy(@SkipInject int stock, @SkipInject int quantity, @SkipInject boolean expected) {
        // arrange
        when(inventory.stockOf("sku-1")).thenReturn(stock);

        // act
        boolean shippable = shipping.canShip("sku-1", quantity);

        // assert
        assertThat(shippable).isEqualTo(expected);
    }

    @Test
    void usesTheProducerOfTheModuleInsteadOfAMock() {
        // act
        int maxQuantity = policy.maxQuantity();

        // assert
        assertThat(maxQuantity).isEqualTo(10);
        assertThat(Arc.container().instance(ShippingPolicies.class).getBean().getScope()).isEqualTo(Dependent.class);
    }

    @Test
    void includesStereotypedBeans() {
        // act
        String label = parcels.label("sku-1");

        // assert
        assertThat(label).isEqualTo("parcel:sku-1");
    }

    @Test
    void includesListenerClassesAsSingletons() {
        // act
        listener.on(new StockReduced("sku-2", 3));

        // assert
        assertThat(listener.lowStock()).containsExactly("sku-2");
        assertThat(Arc.container().instance(ShippingListener.class).getBean().getScope()).isEqualTo(Singleton.class);
    }
}
