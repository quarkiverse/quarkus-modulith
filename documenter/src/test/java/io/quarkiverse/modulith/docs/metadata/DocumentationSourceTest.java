package io.quarkiverse.modulith.docs.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class DocumentationSourceTest {

    static final String JSON = """
            [ {
              "name" : "com.acme.Order",
              "comment" : "An \\"order\\"\\n\\t\\r\\b\\f\\u0041\\/\\\\",
              "methods" : [ {
                "name" : "complete",
                "signature" : "complete(int)",
                "comment" : "Completes it."
              }, {
                "name" : "cancel",
                "signature" : "cancel()",
                "comment" : null
              }, {
                "name" : "blank",
                "signature" : "blank()",
                "comment" : " "
              } ]
            }, {
              "name" : "com.acme.Empty",
              "comment" : "",
              "methods" : [ ]
            }, {
              "name" : "com.acme"
            }, { "name" : "com.acme.Last" } ]
            """;

    private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    @AfterEach
    void restoreContextClassLoader() {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Test
    void readsTypesMethodsAndPackages() {
        // act
        List<TypeMetadata> metadata = DocumentationSource.parse(JSON);

        // assert
        assertThat(metadata).containsExactly(
                new TypeMetadata("com.acme.Order", Optional.of("An \"order\"\n\t\r\b\fA/\\"), List.of(
                        new MethodMetadata("complete", "complete(int)", Optional.of("Completes it.")),
                        new MethodMetadata("cancel", "cancel()", Optional.empty()),
                        new MethodMetadata("blank", "blank()", Optional.of(" ")))),
                new TypeMetadata("com.acme.Empty", Optional.of(""), List.of()),
                new TypeMetadata("com.acme", Optional.empty(), List.of()),
                new TypeMetadata("com.acme.Last", Optional.empty(), List.of()));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "com.acme.Order|complete(int)|Completes it.",
            "com.acme.Order|cancel()|",
            "com.acme.Order|blank()|",
            "com.acme.Order|other()|",
            "com.acme.Other|complete(int)|" })
    void findsMethodDocumentation(String type, String signature, String expected) {
        // arrange
        DocumentationSource source = DocumentationSource.of(DocumentationSource.parse(JSON));

        // act
        Optional<String> documentation = source.getDocumentation(type, signature);

        // assert
        assertThat(documentation).isEqualTo(Optional.ofNullable(expected));
    }

    static Stream<Arguments> typeDocumentation() {
        return Stream.of(Arguments.of("com.acme.Order", Optional.of("An \"order\"\n\t\r\b\fA/\\")),
                Arguments.of("com.acme.Empty", Optional.empty()), Arguments.of("com.acme", Optional.empty()),
                Arguments.of("com.acme.Unknown", Optional.empty()));
    }

    @ParameterizedTest
    @MethodSource("typeDocumentation")
    void findsTypeDocumentation(String type, Optional<String> expected) {
        // arrange
        DocumentationSource source = DocumentationSource.of(DocumentationSource.parse(JSON));

        // act
        Optional<String> documentation = source.getDocumentation(type);

        // assert
        assertThat(documentation).isEqualTo(expected);
    }

    @Test
    void keepsTheFirstEntryOfAName() {
        // arrange
        DocumentationSource source = DocumentationSource.of(List.of(
                new TypeMetadata("com.acme.Order", Optional.of("First."), List.of()),
                new TypeMetadata("com.acme.Order", Optional.of("Second."), List.of())));

        // act
        Optional<String> documentation = source.getDocumentation("com.acme.Order");

        // assert
        assertThat(documentation).contains("First.");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{}|Expected an array for the top level in javadoc.json",
            "[ 1 ]|Invalid javadoc.json: expected a value at offset 2",
            "[ \"x\" ]|Expected an object for a type in javadoc.json",
            "[ { \"methods\" : 1 } ]|Invalid javadoc.json: expected a value at offset 16",
            "[ { \"methods\" : {} } ]|Expected an array for methods in javadoc.json",
            "[ { \"name\" : \"a\", \"methods\" : [ \"x\" ] } ]|Expected an object for a method in javadoc.json",
            "[ { } ]|Missing \"name\" in javadoc.json entry {}",
            "[ { \"name\" : [] } ]|Expected a string for \"name\" in javadoc.json entry {name=[]}",
            "[ ] x|Invalid javadoc.json: expected end of input at offset 4",
            "[ \"a\" \"b\" ]|Invalid javadoc.json: expected ',' or ']' at offset 7",
            "[ { 1 } ]|Invalid javadoc.json: expected a member name at offset 4",
            "[ { \"a\" 1 } ]|Invalid javadoc.json: expected ':' at offset 9",
            "[ { \"a\" : null \"b\" } ]|Invalid javadoc.json: expected ',' or '}' at offset 16",
            "[ \"\\x\" ]|Invalid javadoc.json: expected an escape sequence at offset 5",
            "[ \"\\u12|Invalid javadoc.json: expected four hex digits at offset 5",
            "[ \"\\u12zz\" ]|Invalid javadoc.json: expected four hex digits at offset 9",
            "[|Invalid javadoc.json: expected more input at offset 1",
            "[ \"abc|Invalid javadoc.json: expected more input at offset 6" })
    void rejectsInvalidJson(String json, String message) {
        // act
        Throwable thrown = catchThrowable(() -> DocumentationSource.parse(json));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    @Test
    void readsEveryFileOnTheClassPath() {
        // arrange
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // act
        DocumentationSource source = DocumentationSource.fromClassPath();
        DocumentationSource explicit = DocumentationSource.fromClassLoader(classLoader);

        // assert
        assertThat(source.getDocumentation("com.acme.order")).contains("Takes orders.");
        assertThat(explicit.getDocumentation("com.acme.order")).contains("Takes orders.");
        assertThat(DocumentationSource.none().getDocumentation("com.acme.order")).isEmpty();
    }

    @Test
    void fallsBackToItsOwnClassLoaderWithoutAContextClassLoader() {
        // arrange
        Thread.currentThread().setContextClassLoader(null);

        // act
        DocumentationSource source = DocumentationSource.fromClassPath();

        // assert
        assertThat(source.getDocumentation("com.acme.order")).contains("Takes orders.");
    }

    @Test
    void namesTheInvalidFile(@TempDir Path temp) throws IOException {
        // arrange
        Path file = temp.resolve(DocumentationSource.LOCATION);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{}");
        URL url = file.toUri().toURL();

        // act
        Throwable thrown;
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { temp.toUri().toURL() }, null)) {
            thrown = catchThrowable(() -> DocumentationSource.fromClassLoader(classLoader));
        }

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(url + ": Expected an array for the top level in javadoc.json");
    }

    @Test
    void reportsUnreadableClassPaths() {
        // arrange
        ClassLoader failing = new FailingClassLoader();

        // act
        Throwable thrown = catchThrowable(() -> DocumentationSource.fromClassLoader(failing));

        // assert
        assertThat(thrown).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot read META-INF/quarkus-modulith/javadoc.json").hasRootCauseMessage("broken");
    }
}
