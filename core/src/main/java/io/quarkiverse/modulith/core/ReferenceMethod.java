package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.core.data.ReferenceMethodData;

/**
 * A method of an event listener that receives events, like Spring Modulith's
 * {@code ArchitecturallyEvidentType.ReferenceMethod}.
 */
public final class ReferenceMethod {

    private final JavaType owner;
    private final ReferenceMethodData data;

    ReferenceMethod(JavaType owner, ReferenceMethodData data) {
        this.owner = owner;
        this.data = data;
    }

    /**
     * @return the type declaring the method
     */
    public JavaType getOwner() {
        return owner;
    }

    public String getName() {
        return data.name();
    }

    /**
     * @return the fully qualified names of the parameter types
     */
    public List<String> getParameterTypeNames() {
        return data.parameterTypes();
    }

    /**
     * @return whether the method runs asynchronously, for example an {@code @ApplicationModuleListener}
     */
    public boolean isAsync() {
        return data.async();
    }

    /**
     * @return the transaction phase the method is bound to, if any
     */
    public Optional<String> getTransactionPhase() {
        return Optional.of(data.transactionPhase()).filter(it -> !it.isEmpty());
    }

    /**
     * @return the fully qualified names of the event types the method is interested in
     */
    public List<String> getReferenceTypeNames() {
        return data.referenceTypes();
    }

    @Override
    public String toString() {
        return owner.getName() + "." + data.name() + "(" + String.join(", ", data.parameterTypes()) + ")";
    }
}
