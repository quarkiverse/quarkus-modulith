package io.quarkiverse.modulith.events;

/**
 * Helpers shared by the default externalization configuration and its builders.
 */
final class ExternalizationSupport {

    private ExternalizationSupport() {
    }

    /**
     * @return whether the type is in the package or one of its sub-packages
     */
    static boolean residesIn(Class<?> type, String basePackage) {
        String packageName = type.getPackageName();
        return packageName.equals(basePackage) || packageName.startsWith(basePackage + ".");
    }

    /**
     * Applies the target declared in an {@code Externalized} annotation, keeping the given target and key where the
     * annotation leaves them out.
     */
    static RoutingTarget mergeWithExternalizedAnnotation(Object event, RoutingTarget target) {
        return AnnotationTargetLookup.targetOf(event.getClass())
                .map(declared -> declared.withFallback(target))
                .orElse(target);
    }
}
