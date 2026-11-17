package io.quarkiverse.modulith.apt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModulithJavadocProcessorTest {

    static final String ORDER = """
            package com.acme.order;

            import java.util.List;

            /**
             * An order.
             * It has lines.
             *
             * @author someone
             * @since 1.0
             */
            public class Order {

                /**
                 * Completes the order with the {@code "quoted"} lines.
                 *
                 * @param lines the lines
                 * @return whether it worked
                 */
                public boolean complete(List<String> lines, int[] counts, Line line, String... names) {
                    return true;
                }

                /**
                 * @return only tags
                 */
                public int tagsOnly() {
                    return 0;
                }

                public void undocumented() {
                }

                /** A line. */
                public record Line(String product) {

                    /** The product name. */
                    public String name() {
                        return product;
                    }
                }

                public static class Plain {
                }
            }
            """;

    static final String PACKAGE = """
            /**
             * Takes orders.
             */
            package com.acme.order;
            """;

    static final String UNDOCUMENTED = """
            package com.acme.other;

            public class Undocumented {

                /** Does it. */
                void act() {
                }
            }
            """;

    @Test
    void writesTheJavadocOfTypesMethodsAndPackages(@TempDir Path output) throws IOException {
        // arrange
        List<JavaFileObject> sources = List.of(source("com/acme/order/Order.java", ORDER),
                source("com/acme/order/package-info.java", PACKAGE),
                source("com/acme/other/Undocumented.java", UNDOCUMENTED),
                source("com/acme/other/Empty.java", "package com.acme.other; public class Empty {}"));

        // act
        CompilationResult result = compile(output.resolve("classes"), sources, new ModulithJavadocProcessor());

        // assert
        assertThat(result.success()).isTrue();
        assertThat(Files.readString(output.resolve("classes").resolve(ModulithJavadocProcessor.LOCATION)))
                .isEqualTo("[{\"name\":\"com.acme.order\",\"comment\":\"Takes orders.\"},"
                        + "{\"name\":\"com.acme.order.Order\",\"comment\":\"An order. It has lines.\",\"methods\":["
                        + "{\"name\":\"complete\",\"signature\":\"complete(java.util.List, int[], "
                        + "com.acme.order.Order$Line, java.lang.String[])\",\"comment\":\"Completes the order with the "
                        + "{@code \\\"quoted\\\"} lines.\"}]},"
                        + "{\"name\":\"com.acme.order.Order$Line\",\"comment\":\"A line.\",\"methods\":["
                        + "{\"name\":\"name\",\"signature\":\"name()\",\"comment\":\"The product name.\"}]},"
                        + "{\"name\":\"com.acme.other.Undocumented\",\"methods\":["
                        + "{\"name\":\"act\",\"signature\":\"act()\",\"comment\":\"Does it.\"}]}]\n");
        assertThat(result.notes()).containsExactly("Extracting Javadoc into "
                + output.toRealPath().resolve("classes").resolve(ModulithJavadocProcessor.LOCATION).toUri() + ".");
    }

    @Test
    void writesNothingForTestSources(@TempDir Path output) {
        // arrange
        Path testClasses = output.resolve("target/test-classes");

        // act
        CompilationResult result = compile(testClasses, List.of(source("com/acme/order/Order.java", ORDER)),
                new ModulithJavadocProcessor());

        // assert
        assertThat(result.success()).isTrue();
        assertThat(Files.exists(testClasses.resolve(ModulithJavadocProcessor.LOCATION))).isFalse();
        assertThat(result.notes()).isEmpty();
    }

    @Test
    void reportsWriteFailuresAsErrors(@TempDir Path output) {
        // arrange
        AbstractProcessor failing = new FailingFilerProcessor(new ModulithJavadocProcessor());

        // act
        CompilationResult result = compile(output, List.of(source("com/acme/order/Order.java", ORDER)), failing);

        // assert
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).containsExactly("Cannot write META-INF/quarkus-modulith/javadoc.json: disk full");
    }

    @Test
    void ignoresModuleDescriptors(@TempDir Path output) throws IOException {
        // arrange
        List<JavaFileObject> sources = List.of(source("module-info.java", "/** A module. */ module acme { }"),
                source("com/acme/Thing.java", "package com.acme; /** A thing. */ public class Thing {}"));

        // act
        CompilationResult result = compile(output, sources, new ModulithJavadocProcessor());

        // assert
        assertThat(result.success()).isTrue();
        assertThat(Files.readString(output.resolve(ModulithJavadocProcessor.LOCATION)))
                .isEqualTo("[{\"name\":\"com.acme.Thing\",\"comment\":\"A thing.\"}]\n");
    }

    @ParameterizedTest
    @CsvSource({
            "file:/work/target/test-classes/META-INF/x.json, true",
            "file:/work/target/classes/META-INF/x.json, false",
            "file:/work/build/classes/java/test/META-INF/x.json, true",
            "file:/work/build/classes/java/testFixtures/META-INF/x.json, true",
            "file:/work/build/classes/java/main/META-INF/x.json, false",
            "file:/work/build/classes/kotlin/test/x.json, true" })
    void recognizesTestOutputDirectories(String uri, boolean test) {
        // act
        boolean actual = ModulithJavadocProcessor.pointsToTestOutput(uri);

        // assert
        assertThat(actual).isEqualTo(test);
    }

    @Test
    void escapesJsonStrings() {
        // arrange
        String text = "a\"b\\c\nd\re\tf\u0001g";

        // act
        String quoted = ModulithJavadocProcessor.quote(text);

        // assert
        assertThat(quoted).isEqualTo("\"a\\\"b\\\\c\\nd\\re\\tf\\u0001g\"");
    }

    @Test
    void supportsTheLatestSourceVersion() {
        // arrange
        ModulithJavadocProcessor processor = new ModulithJavadocProcessor();

        // act
        SourceVersion version = processor.getSupportedSourceVersion();

        // assert
        assertThat(version).isEqualTo(SourceVersion.latestSupported());
    }

    @Test
    void cutsTheCommentAtTheFirstBlockTagOnly(@TempDir Path output) throws IOException {
        // arrange
        String source = """
                package com.acme;

                /**
                 * Mentions {@code @param} and @return in the text.
                 *
                 * @throws IllegalStateException never
                 * @deprecated use something else
                 */
                public class Tagged {

                    /**
                     * Fails.
                     *
                     * @exception IllegalStateException always
                     */
                    public void fail() {
                    }
                }
                """;

        // act
        CompilationResult result = compile(output, List.of(source("com/acme/Tagged.java", source)),
                new ModulithJavadocProcessor());

        // assert
        assertThat(result.success()).isTrue();
        assertThat(Files.readString(output.resolve(ModulithJavadocProcessor.LOCATION))).isEqualTo(
                "[{\"name\":\"com.acme.Tagged\",\"comment\":\"Mentions {@code @param} and @return in the text.\","
                        + "\"methods\":[{\"name\":\"fail\",\"signature\":\"fail()\",\"comment\":\"Fails.\"}]}]\n");
    }

    @Test
    void writesTheJavadocOfTypesNestedAtAnyDepth(@TempDir Path output) throws IOException {
        // arrange
        String source = """
                package com.acme;

                public class Order {

                    public static class Line {

                        /** A detail of a line. */
                        public static class Detail {
                        }
                    }
                }
                """;

        // act
        CompilationResult result = compile(output, List.of(source("com/acme/Order.java", source)),
                new ModulithJavadocProcessor());

        // assert
        assertThat(result.success()).isTrue();
        assertThat(Files.readString(output.resolve(ModulithJavadocProcessor.LOCATION)))
                .isEqualTo("[{\"name\":\"com.acme.Order$Line$Detail\",\"comment\":\"A detail of a line.\"}]\n");
    }

    static JavaFileObject source(String path, String content) {
        return new StringSource(path, content);
    }

    static CompilationResult compile(Path output, List<JavaFileObject> sources, AbstractProcessor processor) {
        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(new StringWriter(), null, diagnostics,
                List.of("-d", output.toString(), "-proc:only"), null, sources);
        task.setProcessors(List.of(processor));
        boolean success = task.call();
        List<String> notes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            String message = diagnostic.getMessage(Locale.ROOT);
            if (diagnostic.getKind() == Diagnostic.Kind.NOTE) {
                notes.add(message);
            } else if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(message);
            }
        }
        return new CompilationResult(success, notes, errors);
    }
}
