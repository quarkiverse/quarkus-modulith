package io.quarkiverse.modulith.events.deployment.externalization;

import java.lang.reflect.Modifier;
import java.util.Objects;

import org.jboss.jandex.ClassInfo;

/**
 * What the compiler of one routing expression reports errors against, and the access rules of the generated class,
 * which lives in the package of the event type. Package-private members are only reachable for an event of the
 * application: a library's event is loaded by another class loader than the generated class in dev and test mode, so
 * the two never share a runtime package.
 *
 * @param source the expression as declared
 * @param eventType the event type the expression is compiled for
 * @param packageName the package of the generated class, {@code null} for the default package
 * @param applicationEvent whether the event type is an application class
 */
record CompilationContext(String source, String eventType, String packageName, boolean applicationEvent) {

    RoutingExpressionException error(String reason) {
        return new RoutingExpressionException("Invalid routing expression '" + source + "' for " + eventType + ": "
                + reason);
    }

    /**
     * @return the message of the failure when the value at the path is {@code null} at runtime
     */
    String nullValue(String path) {
        return "Routing expression '" + source + "' for " + eventType + " cannot be evaluated: " + path + " is null";
    }

    /**
     * @throws RoutingExpressionException unless generated code in the event's package may use the type
     */
    void checkAccessible(ClassInfo type) {
        if (!Modifier.isPublic(type.flags()) && !samePackage(type)) {
            throw error(type.name() + " is not accessible, make it public");
        }
    }

    /**
     * @throws RoutingExpressionException unless generated code in the event's package may use the member
     */
    void checkAccessible(ClassInfo declaringClass, short flags, String member) {
        boolean accessible = Modifier.isPublic(flags) || !Modifier.isPrivate(flags) && samePackage(declaringClass);
        if (!accessible) {
            throw error(declaringClass.name() + "." + member + " is not accessible, make it public");
        }
    }

    private boolean samePackage(ClassInfo type) {
        return applicationEvent && Objects.equals(type.name().packagePrefix(), packageName);
    }
}
