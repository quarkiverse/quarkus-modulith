package io.quarkiverse.modulith.apt;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Records the Javadoc of the types, methods and packages of an application in
 * {@code META-INF/quarkus-modulith/javadoc.json}, for the module canvases and the Dev UI. The counterpart of Spring
 * Modulith's {@code SpringModulithProcessor}, writing the same JSON format.
 * <p>
 * Registered as a service, so adding {@code quarkus-modulith-apt} to the annotation processor path activates it. It
 * never claims annotations, and it writes nothing for test sources, like Spring Modulith's processor.
 */
@SupportedAnnotationTypes("*")
public class ModulithJavadocProcessor extends AbstractProcessor {

    /**
     * Where the Javadoc is written, relative to the class output directory.
     */
    public static final String LOCATION = "META-INF/quarkus-modulith/javadoc.json";

    /**
     * The first block tag, such as {@code @param} or {@code @throws}, at the start of a line. Inline tags like
     * {@code {@code @param}} in the text are not matched.
     */
    private static final Pattern BLOCK_TAG = Pattern.compile("(?m)^\\s*@\\w+");

    private final TreeMap<String, TypeJavadoc> metadata = new TreeMap<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            roundEnv.getRootElements().stream().flatMap(this::toMetadata)
                    .forEach(it -> metadata.put(it.name(), it));
            return false;
        }
        try {
            FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", LOCATION);
            if (pointsToTestOutput(resource.toUri().toString())) {
                return false;
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Extracting Javadoc into " + resource.toUri() + ".");
            try (Writer writer = resource.openWriter()) {
                writer.write(toJson(List.copyOf(metadata.values())));
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Cannot write " + LOCATION + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * @return whether the URI points into a Maven or Gradle test output directory
     */
    static boolean pointsToTestOutput(String uri) {
        return uri.matches("(.*/)?target/test-classes/.*")
                || uri.matches("(.*/)?build(/.+)?/classes(/.+)?/(test|testFixtures)/.*");
    }

    private Stream<TypeJavadoc> toMetadata(Element element) {
        if (element instanceof TypeElement type) {
            return withNestedTypes(type).flatMap(this::toTypeMetadata);
        }
        if (element instanceof PackageElement pkg) {
            return Stream.of(new TypeJavadoc(pkg.getQualifiedName().toString(), comment(pkg), List.of()));
        }
        return Stream.empty();
    }

    /**
     * @return the type and its nested types, at any depth
     */
    private static Stream<TypeElement> withNestedTypes(TypeElement type) {
        return Stream.concat(Stream.of(type), ElementFilter.typesIn(type.getEnclosedElements()).stream()
                .flatMap(ModulithJavadocProcessor::withNestedTypes));
    }

    private Stream<TypeJavadoc> toTypeMetadata(TypeElement type) {
        List<MethodJavadoc> methods = new ArrayList<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            String comment = comment(method);
            if (Objects.nonNull(comment)) {
                methods.add(new MethodJavadoc(method.getSimpleName().toString(), signature(method), comment));
            }
        }
        String comment = comment(type);
        return Objects.nonNull(comment) || !methods.isEmpty()
                ? Stream.of(new TypeJavadoc(qualifiedName(type), comment, methods))
                : Stream.empty();
    }

    private String qualifiedName(TypeElement type) {
        if (type.getNestingKind() == NestingKind.MEMBER) {
            return qualifiedName((TypeElement) type.getEnclosingElement()) + "$" + type.getSimpleName();
        }
        return type.getQualifiedName().toString();
    }

    private String comment(Element element) {
        String result = processingEnv.getElementUtils().getDocComment(element);
        if (Objects.isNull(result)) {
            return null;
        }
        Matcher tag = BLOCK_TAG.matcher(result);
        if (tag.find()) {
            result = result.substring(0, tag.start());
        }
        result = result.trim().replaceAll("\\n\\s*", " ");
        return result.isBlank() ? null : result;
    }

    private String signature(ExecutableElement method) {
        return method.getSimpleName() + method.getParameters().stream()
                .map(it -> typeName(it.asType()))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private String typeName(TypeMirror type) {
        TypeMirror erased = processingEnv.getTypeUtils().erasure(type);
        if (erased.getKind() == TypeKind.ARRAY) {
            return typeName(((ArrayType) erased).getComponentType()) + "[]";
        }
        if (erased.getKind() == TypeKind.DECLARED) {
            TypeElement element = (TypeElement) ((DeclaredType) erased).asElement();
            return processingEnv.getElementUtils().getBinaryName(element).toString();
        }
        return erased.toString();
    }

    static String toJson(List<TypeJavadoc> types) {
        return types.stream()
                .sorted(Comparator.comparing(TypeJavadoc::name))
                .map(ModulithJavadocProcessor::toJson)
                .collect(Collectors.joining(",", "[", "]")) + "\n";
    }

    private static String toJson(TypeJavadoc type) {
        StringBuilder json = new StringBuilder("{\"name\":").append(quote(type.name()));
        if (Objects.nonNull(type.comment())) {
            json.append(",\"comment\":").append(quote(type.comment()));
        }
        if (!type.methods().isEmpty()) {
            json.append(",\"methods\":").append(type.methods().stream()
                    .map(it -> "{\"name\":" + quote(it.name()) + ",\"signature\":" + quote(it.signature())
                            + ",\"comment\":" + quote(it.comment()) + "}")
                    .collect(Collectors.joining(",", "[", "]")));
        }
        return json.append('}').toString();
    }

    static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}
