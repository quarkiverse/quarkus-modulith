package io.quarkiverse.modulith.it.moduletests.shipping;

@ShippingComponent
public class Parcels {

    public String label(String sku) {
        return "parcel:" + sku;
    }
}
