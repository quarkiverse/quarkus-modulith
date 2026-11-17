package io.quarkiverse.modulith.core.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.NamedInterfaceData;
import io.quarkiverse.modulith.core.ModelData.ViolationData;

/**
 * Reads what {@link ModelEncoder} wrote. Each read consumes the next component, so the reads follow the record
 * component order exactly.
 */
final class ModelDecoder {

    private final byte[] bytes;
    private final List<String> strings = new ArrayList<>();
    private int position;

    ModelDecoder(byte[] bytes) {
        this.bytes = bytes;
    }

    ModelData model() {
        String systemName = string();
        String basePackage = string();
        List<ModuleData> modules = list(this::module);
        List<ViolationData> violations = list(this::violation);
        String mermaid = string();
        String source = string();
        List<String> rootPackages = strings();
        List<String> sharedModules = strings();
        boolean fullyQualified = bool();
        List<TypeData> types = list(this::type);
        return new ModelData(systemName, basePackage, modules, violations, mermaid, source, rootPackages, sharedModules,
                fullyQualified, types, bool());
    }

    private ModuleData module() {
        String name = string();
        String displayName = string();
        String basePackage = string();
        boolean open = bool();
        boolean restricted = bool();
        List<String> allowed = strings();
        List<NamedInterfaceData> namedInterfaces = list(this::namedInterface);
        List<String> dependencies = strings();
        List<String> published = strings();
        List<String> listened = strings();
        List<String> types = strings();
        List<String> excluded = strings();
        List<DependencyData> qualified = list(this::dependency);
        List<String> beans = strings();
        List<EventTypeData> events = list(this::event);
        List<String> aggregateRoots = strings();
        List<String> valueTypes = strings();
        return new ModuleData(name, displayName, basePackage, open, restricted, allowed, namedInterfaces, dependencies,
                published, listened, types, excluded, qualified, beans, events, aggregateRoots, valueTypes,
                list(this::entryPoint));
    }

    private NamedInterfaceData namedInterface() {
        String name = string();
        List<String> packages = strings();
        return new NamedInterfaceData(name, packages, strings());
    }

    private ViolationData violation() {
        String kind = string();
        String module = string();
        String source = string();
        String target = string();
        return new ViolationData(kind, module, source, target, string());
    }

    private DependencyData dependency() {
        String source = string();
        String target = string();
        String type = string();
        String description = string();
        return new DependencyData(source, target, type, description, string());
    }

    private EventTypeData event() {
        String type = string();
        return new EventTypeData(type, list(this::eventSource));
    }

    private EventSourceData eventSource() {
        String type = string();
        String method = string();
        return new EventSourceData(type, method, bool());
    }

    private EntryPointData entryPoint() {
        String type = string();
        String method = string();
        String kind = string();
        return new EntryPointData(type, method, kind, string());
    }

    private TypeData type() {
        String name = string();
        int modifiers = number();
        List<String> supertypes = strings();
        List<String> interfaces = strings();
        List<String> annotations = strings();
        List<String> metaAnnotations = strings();
        List<String> signatureTypes = strings();
        List<AnnotationData> declarations = list(this::annotation);
        List<String> stereotypes = strings();
        return new TypeData(name, modifiers, supertypes, interfaces, annotations, metaAnnotations, signatureTypes,
                declarations, stereotypes, list(this::referenceMethod));
    }

    private AnnotationData annotation() {
        String type = string();
        return new AnnotationData(type, list(this::attribute));
    }

    private AttributeData attribute() {
        String name = string();
        return new AttributeData(name, strings());
    }

    private ReferenceMethodData referenceMethod() {
        String name = string();
        List<String> parameterTypes = strings();
        boolean async = bool();
        String phase = string();
        return new ReferenceMethodData(name, parameterTypes, async, phase, strings());
    }

    private <T> List<T> list(Supplier<T> item) {
        return IntStream.range(0, number()).mapToObj(index -> item.get()).toList();
    }

    private List<String> strings() {
        return list(this::string);
    }

    private String string() {
        int index = number();
        if (index > 0) {
            return strings.get(index - 1);
        }
        int length = number();
        String value = new String(bytes, position, length, StandardCharsets.UTF_8);
        position += length;
        strings.add(value);
        return value;
    }

    private boolean bool() {
        return bytes[position++] == 1;
    }

    private int number() {
        int value = 0;
        int shift = 0;
        int next;
        do {
            next = bytes[position++];
            value |= (next & 0x7f) << shift;
            shift += 7;
        } while ((next & 0x80) != 0);
        return value;
    }
}
