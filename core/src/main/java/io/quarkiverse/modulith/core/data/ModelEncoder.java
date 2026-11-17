package io.quarkiverse.modulith.core.data;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.NamedInterfaceData;
import io.quarkiverse.modulith.core.ModelData.ViolationData;

/**
 * Writes a {@link ModelData} for {@link ModelDecoder}, component by component in declaration order.
 */
final class ModelEncoder {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Map<String, Integer> strings = new HashMap<>();

    ModelEncoder model(ModelData data) {
        string(data.systemName());
        string(data.basePackage());
        list(data.modules(), this::module);
        list(data.violations(), this::violation);
        string(data.mermaid());
        string(data.source());
        strings(data.rootPackages());
        strings(data.sharedModules());
        bool(data.useFullyQualifiedModuleNames());
        list(data.types(), this::type);
        bool(data.verified());
        return this;
    }

    byte[] toByteArray() {
        return out.toByteArray();
    }

    private void module(ModuleData data) {
        string(data.name());
        string(data.displayName());
        string(data.basePackage());
        bool(data.open());
        bool(data.restricted());
        strings(data.allowedDependencies());
        list(data.namedInterfaces(), this::namedInterface);
        strings(data.dependencies());
        strings(data.publishedEvents());
        strings(data.listenedEvents());
        strings(data.types());
        strings(data.excludedPackages());
        list(data.qualifiedDependencies(), this::dependency);
        strings(data.beans());
        list(data.events(), this::event);
        strings(data.aggregateRoots());
        strings(data.valueTypes());
        list(data.entryPoints(), this::entryPoint);
    }

    private void namedInterface(NamedInterfaceData data) {
        string(data.name());
        strings(data.packages());
        strings(data.types());
    }

    private void violation(ViolationData data) {
        string(data.kind());
        string(data.module());
        string(data.source());
        string(data.target());
        string(data.message());
    }

    private void dependency(DependencyData data) {
        string(data.source());
        string(data.target());
        string(data.type());
        string(data.description());
        string(data.injectedField());
    }

    private void event(EventTypeData data) {
        string(data.type());
        list(data.sources(), source -> {
            string(source.type());
            string(source.method());
            bool(source.parameters());
        });
    }

    private void entryPoint(EntryPointData data) {
        string(data.type());
        string(data.method());
        string(data.kind());
        string(data.detail());
    }

    private void type(TypeData data) {
        string(data.name());
        number(data.modifiers());
        strings(data.supertypes());
        strings(data.interfaces());
        strings(data.annotations());
        strings(data.metaAnnotations());
        strings(data.signatureTypes());
        list(data.declarations(), this::annotation);
        strings(data.stereotypes());
        list(data.referenceMethods(), this::referenceMethod);
    }

    private void annotation(AnnotationData data) {
        string(data.type());
        list(data.attributes(), attribute -> {
            string(attribute.name());
            strings(attribute.values());
        });
    }

    private void referenceMethod(ReferenceMethodData data) {
        string(data.name());
        strings(data.parameterTypes());
        bool(data.async());
        string(data.transactionPhase());
        strings(data.referenceTypes());
    }

    private <T> void list(List<T> items, Consumer<T> item) {
        number(items.size());
        items.forEach(item);
    }

    private void strings(List<String> items) {
        list(items, this::string);
    }

    /**
     * Writes the index of a string seen before plus one, or zero followed by the UTF-8 bytes of a new one.
     */
    private void string(String value) {
        Integer known = strings.get(value);
        if (Objects.nonNull(known)) {
            number(known + 1);
            return;
        }
        strings.put(value, strings.size());
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        number(0);
        number(bytes.length);
        out.write(bytes, 0, bytes.length);
    }

    private void bool(boolean value) {
        out.write(value ? 1 : 0);
    }

    /**
     * Writes a non-negative number in seven-bit groups, low group first.
     */
    private void number(int value) {
        int rest = value;
        while (rest >= 0x80) {
            out.write(rest & 0x7f | 0x80);
            rest >>>= 7;
        }
        out.write(rest);
    }
}
