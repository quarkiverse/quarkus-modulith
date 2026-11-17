package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

/**
 * The beans of the application that routing expressions may call by name: classes, producer methods and producer
 * fields annotated with {@link Named}, with CDI's default names.
 */
final class NamedBeans {

    private static final DotName NAMED = DotName.createSimple(Named.class);
    private static final DotName PRODUCES = DotName.createSimple(Produces.class);

    private NamedBeans() {
    }

    /**
     * @return the bean types by bean name
     */
    static Map<String, Type> scan(IndexView index) {
        Map<String, Type> beans = new TreeMap<>();
        for (AnnotationInstance named : index.getAnnotations(NAMED)) {
            Optional<String> declared = Optional.ofNullable(named.value()).map(AnnotationValue::asString)
                    .filter(name -> !name.isBlank());
            AnnotationTarget target = named.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                beans.put(declared.orElse(decapitalize(target.asClass().simpleName())),
                        ClassType.create(target.asClass().name()));
            } else if (target.kind() == AnnotationTarget.Kind.METHOD
                    && target.asMethod().hasDeclaredAnnotation(PRODUCES)) {
                beans.put(declared.orElse(propertyName(target.asMethod().name())), target.asMethod().returnType());
            } else if (target.kind() == AnnotationTarget.Kind.FIELD && target.asField().hasDeclaredAnnotation(PRODUCES)) {
                beans.put(declared.orElse(target.asField().name()), target.asField().type());
            }
        }
        return beans;
    }

    /**
     * @return the default name of a producer method: the property name for getters, the method name otherwise
     */
    static String propertyName(String methodName) {
        boolean getter = methodName.length() > 3 && methodName.startsWith("get");
        return getter ? decapitalize(methodName.substring(3)) : methodName;
    }

    private static String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
