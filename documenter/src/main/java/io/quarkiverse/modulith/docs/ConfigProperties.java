package io.quarkiverse.modulith.docs;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * Lists the configuration properties a type declares in a plain test, as the build lists them from the Jandex index:
 * the properties of a {@code @ConfigMapping} interface, with nested groups, {@code @WithName}, {@code @WithDefault},
 * {@code @WithParentName} and the Javadoc of the methods, and the {@code @ConfigProperty} injection points of a bean.
 * The Quarkus counterpart of the {@code spring-configuration-metadata.json} Spring Modulith's documenter reads.
 * <p>
 * The types are loaded without being initialized, and the annotations are matched by name, so the documenter needs
 * neither SmallRye Config nor MicroProfile Config on its class path.
 */
final class ConfigProperties {

    static final String CONFIG_MAPPING = "io.smallrye.config.ConfigMapping";
    static final String WITH_NAME = "io.smallrye.config.WithName";
    static final String WITH_DEFAULT = "io.smallrye.config.WithDefault";
    static final String WITH_PARENT_NAME = "io.smallrye.config.WithParentName";
    static final String CONFIG_PROPERTY = "org.eclipse.microprofile.config.inject.ConfigProperty";
    static final String UNCONFIGURED_VALUE = "org.eclipse.microprofile.config.configproperty.unconfigureddvalue";

    private final DocumentationSource javadoc;

    ConfigProperties(DocumentationSource javadoc) {
        this.javadoc = javadoc;
    }

    /**
     * @return the properties the type declares, none when the type or a type it refers to cannot be loaded
     */
    List<ModuleProperty> of(JavaType type) {
        try {
            return of(type.reflect());
        } catch (IllegalStateException | LinkageError e) {
            return List.of();
        }
    }

    private List<ModuleProperty> of(Class<?> type) {
        List<ModuleProperty> result = new ArrayList<>();
        annotation(type, CONFIG_MAPPING).ifPresent(mapping -> mappingProperties(type,
                value(mapping, "prefix").orElse(""), result));
        Arrays.stream(type.getDeclaredFields()).forEach(field -> injectionPoint(type, field, field.getGenericType(),
                field.getName(), result));
        Stream.concat(Arrays.stream(type.getDeclaredConstructors()), Arrays.stream(type.getDeclaredMethods()))
                .flatMap(it -> Arrays.stream(it.getParameters()))
                .forEach(parameter -> injectionPoint(type, parameter, parameter.getParameterizedType(),
                        parameter.getName(), result));
        return result;
    }

    /**
     * Lists the properties of a mapping or group, those its super-interfaces declare included. SmallRye Config rejects
     * groups that contain themselves, so the recursion ends.
     */
    private void mappingProperties(Class<?> group, String prefix, List<ModuleProperty> result) {
        Arrays.stream(group.getInterfaces()).forEach(parent -> mappingProperties(parent, prefix, result));
        List<Method> methods = Arrays.stream(group.getDeclaredMethods())
                .filter(it -> it.getParameterCount() == 0 && Modifier.isAbstract(it.getModifiers()))
                .sorted(Comparator.comparing(Method::getName))
                .toList();
        for (Method method : methods) {
            String name = annotation(method, WITH_PARENT_NAME).isPresent() ? prefix
                    : join(prefix, annotation(method, WITH_NAME).flatMap(it -> value(it, "value"))
                            .orElseGet(() -> kebab(method.getName())));
            Type returned = method.getGenericReturnType();
            Optional<Class<?>> nested = groupOf(returned);
            if (nested.isPresent()) {
                mappingProperties(nested.get(), name, result);
            } else {
                result.add(new ModuleProperty(name, returned.getTypeName(), group.getName(),
                        javadoc.getDocumentation(group.getName(), method.getName() + "()"),
                        annotation(method, WITH_DEFAULT).flatMap(it -> value(it, "value"))));
            }
        }
    }

    private static String join(String prefix, String name) {
        return prefix.isEmpty() ? name : prefix + "." + name;
    }

    /**
     * @return the group interface the type stands for, unwrapping {@code Optional}; JDK interfaces such as collections
     *         are leaves
     */
    static Optional<Class<?>> groupOf(Type type) {
        Type candidate = type;
        if (type instanceof ParameterizedType parameterized
                && parameterized.getRawType().getTypeName().equals("java.util.Optional")) {
            candidate = parameterized.getActualTypeArguments()[0];
        }
        if (!(candidate instanceof Class<?> group) || !group.isInterface() || group.getName().startsWith("java.")) {
            return Optional.empty();
        }
        return Optional.of(group);
    }

    private static void injectionPoint(Class<?> owner, AnnotatedElement element, Type type, String member,
            List<ModuleProperty> result) {
        annotation(element, CONFIG_PROPERTY).ifPresent(annotation -> {
            String name = value(annotation, "name").filter(it -> !it.isBlank())
                    .orElseGet(() -> owner.getName() + "." + member);
            Optional<String> defaultValue = value(annotation, "defaultValue")
                    .filter(it -> !UNCONFIGURED_VALUE.equals(it));
            result.add(new ModuleProperty(name, type.getTypeName(), owner.getName(), Optional.empty(), defaultValue));
        });
    }

    private static Optional<Annotation> annotation(AnnotatedElement element, String name) {
        return Arrays.stream(element.getDeclaredAnnotations())
                .filter(it -> it.annotationType().getName().equals(name))
                .findFirst();
    }

    /**
     * @return the value of an attribute of the annotation, empty when the annotation has no such attribute
     */
    static Optional<String> value(Annotation annotation, String attribute) {
        try {
            return Optional.of(String.valueOf(annotation.annotationType().getMethod(attribute).invoke(annotation)));
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }

    /**
     * @return the kebab-case form SmallRye Config derives from a method name: {@code maxSize} gives {@code max-size}
     */
    static String kebab(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }
}
