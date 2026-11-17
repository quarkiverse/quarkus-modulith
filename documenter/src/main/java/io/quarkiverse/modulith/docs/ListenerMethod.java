package io.quarkiverse.modulith.docs;

import java.util.List;

/**
 * An event listener method of a bean.
 *
 * @param name the method name
 * @param parameterTypes the binary names of the parameter types, used to find the method's Javadoc
 * @param referenceTypes the event types the method listens to
 * @param async whether the method runs asynchronously
 */
public record ListenerMethod(String name, List<String> parameterTypes, List<String> referenceTypes, boolean async) {

    public ListenerMethod {
        parameterTypes = List.copyOf(parameterTypes);
        referenceTypes = List.copyOf(referenceTypes);
    }

    /**
     * @return the method signature as written by the Javadoc annotation processor: {@code name(a.B, c.D)}
     */
    public String signature() {
        return name + "(" + String.join(", ", parameterTypes) + ")";
    }
}
