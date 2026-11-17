package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.VoidType;
import org.jboss.jandex.WildcardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.GenericHolder;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keyed;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.KeyedEvent;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keys;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Labeled;

class RoutingExpressionCompilerTest {

    private static final String EVENT = CompileEvent.class.getName();
    private static final String OTHER = "io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.other.";

    private final List<String> written = new ArrayList<>();

    private static Index index() throws IOException {
        Indexer indexer = new Indexer();
        for (String name : List.of(EVENT, Labeled.class.getName(), GenericHolder.class.getName(), Keys.class.getName(),
                Keyed.class.getName(), KeyedEvent.class.getName(), Object.class.getName(), String.class.getName(),
                CharSequence.class.getName(), OTHER + "BaseWithHidden",
                "io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.LocalThing",
                OTHER + "Internal")) {
            try (InputStream in = RoutingExpressionCompilerTest.class.getClassLoader()
                    .getResourceAsStream(name.replace('.', '/') + ".class")) {
                indexer.index(in);
            }
        }
        return indexer.complete();
    }

    private RoutingExpressionCompiler compiler() throws IOException {
        return compiler(true);
    }

    private RoutingExpressionCompiler compiler(boolean applicationEvents) throws IOException {
        Index index = index();
        return new RoutingExpressionCompiler(index, NamedBeans.scan(index), (name, bytes) -> written.add(name),
                name -> applicationEvents);
    }

    private String compile(String expression) throws IOException {
        RoutingExpressionCompiler compiler = compiler();
        ClassInfo event = Index.of(CompileEvent.class).getClassByName(DotName.createSimple(EVENT));
        return compiler.compile(event, RoutingExpressionParser.parse(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{event}", "{event.name}", "{event.active}", "{event.code}",
            "{event.count}", "{event.local()}", "{event.labeled.label}", "{event.holder.content.length()}",
            "{inject:keys.of(event)}", "{inject:keys.forEvent(event)}", "{inject:keys.forLabel(event)}",
            "{inject:keys.count(event.count)}", "{inject:keys.text(event.name).length()}", "a-{event.name}-b",
            "{inject:keys.pick(event).label}", "{inject:keys.either(event, event)}",
            "plain", "{event.thing.value}",
            "#{#this.getName()}" })
    void compilesSupportedExpressions(String expression) throws IOException {
        // act
        String generated = compile(expression);

        // assert
        assertThat(generated).isEqualTo(EVENT + RoutingExpressionCompiler.SUFFIX + "0");
        assertThat(written).containsExactly(generated.replace('.', '/'));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{event.missing}|"
                    + "io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent has no accessible property missing",
            "{event.boxed}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent has no accessible property boxed",
            "{event.counted}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent has no accessible property counted",
            "{event.STATIC_FIELD}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent has no accessible property STATIC_FIELD",
            "{inject:keys.of()}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keys has no accessible method of taking 0 matching argument(s)",
            "{event.missing()}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent has no accessible method missing taking 0 matching argument(s)",
            "{event.staticOne()}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent has no accessible method staticOne taking 0 matching argument(s)",
            "{event.secret}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent.secret is not accessible, make it public",
            "{event.privy()}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.CompileEvent.privy() is not accessible, make it public",
            "{event.hidden()}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.other.BaseWithHidden.hidden() is not accessible, make it public",
            "{event.internal.value}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.other.Internal is not accessible, make it public",
            "{event.count.value}|cannot access members of the int value",
            "{event.tags.length}|cannot access members of the int[] value",
            "{event.duration.seconds}|the type java.time.Duration is not known to the build",
            "{inject:nobody.of(event)}|there is no bean named nobody, declare one with @Named(\"nobody\")",
            "{inject:keys.count(event)}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keys has no accessible method count taking 1 matching argument(s)",
            "{inject:keys.text(event)}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keys has no accessible method text taking 1 matching argument(s)",
            "{inject:keys.text(event.duration)}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keys has no accessible method text taking 1 matching argument(s)",
            "{inject:keys.of(event.count)}|io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.Keys has no accessible method of taking 1 matching argument(s)"
    })
    void rejectsMembersTheGeneratedCodeCannotReach(String expression, String reason) {
        // act & assert
        assertThatThrownBy(() -> compile(expression)).isInstanceOf(RoutingExpressionException.class)
                .hasMessage("Invalid routing expression '" + expression + "' for " + EVENT + ": " + reason);

        // assert
        assertThat(written).isEmpty();
    }

    @Test
    void reachesOnlyPublicMembersOfLibraryEvents() throws IOException {
        // arrange
        RoutingExpressionCompiler compiler = compiler(false);
        ClassInfo event = Index.of(CompileEvent.class).getClassByName(DotName.createSimple(EVENT));

        // act & assert
        assertThatThrownBy(() -> compiler.compile(event, RoutingExpressionParser.parse("{event.local()}")))
                .isInstanceOf(RoutingExpressionException.class)
                .hasMessage("Invalid routing expression '{event.local()}' for " + EVENT + ": " + EVENT
                        + ".local() is not accessible, make it public");
    }

    @Test
    void skipsBridgeMethods() throws IOException {
        // arrange
        RoutingExpressionCompiler compiler = compiler();
        ClassInfo event = Index.of(KeyedEvent.class).getClassByName(DotName.createSimple(KeyedEvent.class));

        // act
        String generated = compiler.compile(event, RoutingExpressionParser.parse("{event.key.label}"));

        // assert
        assertThat(generated).isEqualTo(KeyedEvent.class.getName() + RoutingExpressionCompiler.SUFFIX + "0");
    }

    @Test
    void readsTheEventTypeOfAGeneratedClass() {
        // act
        String eventType = RoutingExpressionCompiler.eventTypeOf("com/acme/Order" + RoutingExpressionCompiler.SUFFIX + "12");

        // assert
        assertThat(eventType).isEqualTo("com/acme/Order");
    }

    @Test
    void numbersTheGeneratedClasses() throws IOException {
        // arrange
        RoutingExpressionCompiler compiler = compiler();
        ClassInfo event = Index.of(CompileEvent.class).getClassByName(DotName.createSimple(EVENT));

        // act
        String first = compiler.compile(event, RoutingExpressionParser.parse("{event.name}"));
        String second = compiler.compile(event, RoutingExpressionParser.parse("{event.code}"));

        // assert
        assertThat(first).endsWith("$$ModulithRouting0");
        assertThat(second).endsWith("$$ModulithRouting1");
    }

    @Test
    void erasesTypesToTheirBounds() {
        // act
        DotName unbounded = RoutingExpressionCompiler.erasure(TypeVariable.create("T"));
        DotName bounded = RoutingExpressionCompiler.erasure(
                TypeVariable.builder("T").addBound(ClassType.create(CharSequence.class)).build());
        DotName wildcard = RoutingExpressionCompiler.erasure(WildcardType.UNBOUNDED);
        DotName plain = RoutingExpressionCompiler.erasure(ClassType.create(String.class));

        // assert
        assertThat(unbounded).isEqualTo(DotName.createSimple(Object.class));
        assertThat(bounded).isEqualTo(DotName.createSimple(CharSequence.class));
        assertThat(wildcard).isEqualTo(DotName.createSimple(Object.class));
        assertThat(plain).isEqualTo(DotName.createSimple(String.class));
    }

    static Stream<Arguments> describesPrimitives() {
        return Stream.of(Arguments.of(PrimitiveType.BOOLEAN, "Z"), Arguments.of(PrimitiveType.BYTE, "B"),
                Arguments.of(PrimitiveType.CHAR, "C"), Arguments.of(PrimitiveType.DOUBLE, "D"),
                Arguments.of(PrimitiveType.FLOAT, "F"), Arguments.of(PrimitiveType.INT, "I"),
                Arguments.of(PrimitiveType.LONG, "J"), Arguments.of(PrimitiveType.SHORT, "S"));
    }

    @ParameterizedTest
    @MethodSource
    void describesPrimitives(PrimitiveType type, String descriptor) {
        // act
        String described = RoutingExpressionCompiler.descriptor(type);

        // assert
        assertThat(described).isEqualTo(descriptor);
    }

    @Test
    void describesOtherTypes() {
        // act
        String voidType = RoutingExpressionCompiler.descriptor(VoidType.VOID);
        String array = RoutingExpressionCompiler.descriptor(ArrayType.create(PrimitiveType.INT, 2));
        String string = RoutingExpressionCompiler.descriptor(ClassType.create(String.class));
        String variable = RoutingExpressionCompiler.descriptor(TypeVariable.create("T"));

        // assert
        assertThat(voidType).isEqualTo("V");
        assertThat(array).isEqualTo("[[I");
        assertThat(string).isEqualTo("Ljava/lang/String;");
        assertThat(variable).isEqualTo("Ljava/lang/Object;");
    }

    @Test
    void comparesPrimitivesExactly() throws IOException {
        // arrange
        RoutingExpressionCompiler compiler = compiler();

        // act
        boolean samePrimitive = compiler.isAssignable(PrimitiveType.INT, PrimitiveType.INT);
        boolean otherPrimitive = compiler.isAssignable(PrimitiveType.INT, PrimitiveType.LONG);
        boolean referenceToPrimitive = compiler.isAssignable(ClassType.create(String.class), PrimitiveType.INT);
        boolean toInterface = compiler.isAssignable(ClassType.create(CompileEvent.class),
                ClassType.create(Labeled.class));
        boolean primitiveToObject = compiler.isAssignable(PrimitiveType.INT, ClassType.create(Object.class));

        // assert
        assertThat(samePrimitive).isTrue();
        assertThat(otherPrimitive).isFalse();
        assertThat(referenceToPrimitive).isFalse();
        assertThat(toInterface).isTrue();
        assertThat(primitiveToObject).isFalse();
    }
}
