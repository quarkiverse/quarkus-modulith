package io.quarkiverse.modulith.deployment.docs;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.docs.ModuleProperty;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * Lists the configuration properties a class declares: the properties of a {@code @ConfigMapping} interface, with
 * nested groups, and the {@code @ConfigProperty} injection points of a
 * bean. The Quarkus counterpart of the
 * {@code spring-configuration-metadata.json} Spring Modulith reads.
 */
final class ConfigPropertyCollector {

    private static final Set<String> LEAF_WRAPPERS = Set.of("java.util.Optional", "java.util.List", "java.util.Set",
            "java.util.Map");

    private final TypeInspector types;
    private final DocumentationSource javadoc;

    ConfigPropertyCollector(TypeInspector types, DocumentationSource javadoc) {
        this.types = types;
        this.javadoc = javadoc;
    }

    List<ModuleProperty> collect(ClassInfo type) {
        List<ModuleProperty> result = new ArrayList<>();
        TypeInspector.annotation(type, Names.CONFIG_MAPPING).ifPresent(mapping -> mappingProperties(type,
                Optional.ofNullable(mapping.value("prefix")).map(AnnotationValue::asString).orElse(""), result));
        for (FieldInfo field : type.fields()) {
            injectionPoint(type, field.annotation(Names.CONFIG_PROPERTY), field.type(), field.name(), result);
        }
        for (MethodInfo method : type.methods()) {
            for (MethodParameterInfo parameter : method.parameters()) {
                injectionPoint(type, parameter.declaredAnnotation(Names.CONFIG_PROPERTY), parameter.type(),
                        parameter.name(), result);
            }
        }
        return result;
    }

    /**
     * Lists the properties of a mapping or group, those its super-interfaces declare included. SmallRye Config rejects
     * groups that contain themselves, so the recursion ends.
     */
    private void mappingProperties(ClassInfo group, String prefix, List<ModuleProperty> result) {
        group.interfaceNames().stream().map(it -> types.classInfo(it.toString())).flatMap(Optional::stream)
                .forEach(parent -> mappingProperties(parent, prefix, result));
        for (MethodInfo method : group.methods()) {
            if (method.parametersCount() > 0 || !Modifier.isAbstract(method.flags())) {
                continue;
            }
            String name = method.hasDeclaredAnnotation(Names.WITH_PARENT_NAME) ? prefix
                    : join(prefix, Optional.ofNullable(method.annotation(Names.WITH_NAME))
                            .map(it -> it.value().asString()).orElseGet(() -> kebab(method.name())));
            Type returned = method.returnType();
            Optional<ClassInfo> nested = groupOf(returned);
            if (nested.isPresent()) {
                mappingProperties(nested.get(), name, result);
                continue;
            }
            result.add(new ModuleProperty(name, returned.toString(), group.name().toString(),
                    javadoc.getDocumentation(group.name().toString(), method.name() + "()"),
                    Optional.ofNullable(method.annotation(Names.WITH_DEFAULT)).map(it -> it.value().asString())));
        }
    }

    private static String join(String prefix, String name) {
        return prefix.isEmpty() ? name : prefix + "." + name;
    }

    /**
     * @return the group interface the type stands for, unwrapping {@code Optional}
     */
    private Optional<ClassInfo> groupOf(Type type) {
        Type candidate = type;
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE && type.name().toString().equals("java.util.Optional")) {
            candidate = type.asParameterizedType().arguments().get(0);
        }
        String name = candidate.name().toString();
        if (LEAF_WRAPPERS.contains(name) || name.startsWith("java.")) {
            return Optional.empty();
        }
        return types.classInfo(name).filter(ClassInfo::isInterface);
    }

    private void injectionPoint(ClassInfo owner, AnnotationInstance annotation, Type type, String member,
            List<ModuleProperty> result) {
        if (Objects.isNull(annotation)) {
            return;
        }
        String name = Optional.ofNullable(annotation.value("name")).map(AnnotationValue::asString)
                .filter(it -> !it.isBlank()).orElseGet(() -> owner.name().toString() + "." + member);
        Optional<String> defaultValue = Optional.ofNullable(annotation.value("defaultValue"))
                .map(AnnotationValue::asString).filter(it -> !Names.UNCONFIGURED_VALUE.equals(it));
        result.add(new ModuleProperty(name, type.toString(), owner.name().toString(), Optional.empty(), defaultValue));
    }

    /**
     * @return the kebab-case form SmallRye Config derives from a method name: {@code maxSize} gives {@code max-size}
     */
    static String kebab(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }
}
