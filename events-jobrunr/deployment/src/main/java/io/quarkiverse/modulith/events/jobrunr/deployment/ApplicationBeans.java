package io.quarkiverse.modulith.events.jobrunr.deployment;

import jakarta.enterprise.inject.Produces;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

/**
 * Finds the beans of a type the application declares, as classes or producers, before the bean discovery. The
 * {@code @Produces} annotation only targets methods and fields.
 */
final class ApplicationBeans {

    private static final DotName PRODUCES = DotName.createSimple(Produces.class);

    private ApplicationBeans() {
    }

    /**
     * @return whether the application declares a bean of the given interface
     */
    static boolean declare(IndexView index, DotName type) {
        if (!index.getAllKnownImplementations(type).isEmpty()) {
            return true;
        }
        for (AnnotationInstance produces : index.getAnnotations(PRODUCES)) {
            AnnotationTarget target = produces.target();
            Type produced = target.kind() == AnnotationTarget.Kind.METHOD ? target.asMethod().returnType()
                    : target.asField().type();
            if (produced.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
