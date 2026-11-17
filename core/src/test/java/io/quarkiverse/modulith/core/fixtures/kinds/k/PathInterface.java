package io.quarkiverse.modulith.core.fixtures.kinds.k;

@jakarta.ws.rs.Path("/plain")
public interface PathInterface {
    @jakarta.ws.rs.GET
    String get();
}
