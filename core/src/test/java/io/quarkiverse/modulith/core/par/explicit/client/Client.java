package io.quarkiverse.modulith.core.par.explicit.client;

import io.quarkiverse.modulith.core.par.explicit.store.Store;
import io.quarkiverse.modulith.core.par.explicit.store.api.StoreApi;
import io.quarkiverse.modulith.core.par.explicit.store.spi.StoreSpi;

public class Client {
    StoreApi api;
    StoreSpi spi;
    Store store;
}
