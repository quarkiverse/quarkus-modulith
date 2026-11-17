package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class RoutingExpressionParserTest {

    private static final ExpressionChain EVENT = new ExpressionChain("", List.of());

    static Stream<Arguments> parsesSupportedForms() {
        ExpressionChain id = new ExpressionChain("", List.of(ExpressionMember.property("id")));
        ExpressionChain keysOfEvent = new ExpressionChain("keys",
                List.of(ExpressionMember.call("of", List.of(EVENT))));
        return Stream.of(
                Arguments.of("{event}", List.of(new ChainSegment(EVENT))),
                Arguments.of("#{#this}", List.of(new ChainSegment(EVENT))),
                Arguments.of("#{#root}", List.of(new ChainSegment(EVENT))),
                Arguments.of("{event.id}", List.of(new ChainSegment(id))),
                Arguments.of("#{#this.id}", List.of(new ChainSegment(id))),
                Arguments.of("#{#this.getLastname()}", List.of(new ChainSegment(new ExpressionChain("",
                        List.of(ExpressionMember.call("getLastname", List.of())))))),
                Arguments.of("{ event . customer . name }", List.of(new ChainSegment(new ExpressionChain("",
                        List.of(ExpressionMember.property("customer"), ExpressionMember.property("name")))))),
                Arguments.of("{inject:keys.of(event)}", List.of(new ChainSegment(keysOfEvent))),
                Arguments.of("#{@keys.of(#this)}", List.of(new ChainSegment(keysOfEvent))),
                Arguments.of("#{@keys.of( #this , #this.id )}", List.of(new ChainSegment(new ExpressionChain("keys",
                        List.of(ExpressionMember.call("of", List.of(EVENT, id))))))),
                Arguments.of("{inject:keys.next()}", List.of(new ChainSegment(new ExpressionChain("keys",
                        List.of(ExpressionMember.call("next", List.of())))))),
                Arguments.of("#{@keys.of(#this).trim()}", List.of(new ChainSegment(new ExpressionChain("keys",
                        List.of(ExpressionMember.call("of", List.of(EVENT)), ExpressionMember.call("trim", List.of())))))),
                Arguments.of("{event.id}-x-#{#this.id}", List.of(new ChainSegment(id), new LiteralSegment("-x-"),
                        new ChainSegment(id))),
                Arguments.of("a-{event}", List.of(new LiteralSegment("a-"), new ChainSegment(EVENT))),
                Arguments.of("plain", List.of(new LiteralSegment("plain"))));
    }

    @ParameterizedTest
    @MethodSource
    void parsesSupportedForms(String source, List<TemplateSegment> segments) {
        // act
        ExpressionTemplate template = RoutingExpressionParser.parse(source);

        // assert
        assertThat(template).isEqualTo(new ExpressionTemplate(source, segments));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{event.id|an expression has no closing }",
            "{}|a name is expected at ''",
            "#{}|an expression starts with #this, #root or @bean",
            "#{T(java.util.UUID).randomUUID()}|an expression starts with #this, #root or @bean",
            "{customer.id}|an expression starts with event or inject:bean",
            "{inject:keys}|the bean keys must be followed by a method call",
            "#{@keys}|the bean keys must be followed by a method call",
            "{inject:keys.prefix}|the bean must be followed by a method call, not by the property prefix",
            "{inject:keys.of(inject:other.next())}|method arguments must be the event or one of its properties",
            "#{@keys.of(@other.next())}|method arguments must be the event or one of its properties",
            "{event.format(event)}|only methods of beans take arguments, format is called on the event",
            "{inject:keys.of(event}|a method call has no closing )",
            "{event.id extra}|unexpected 'extra'",
            "{event.}|a name is expected at ''",
            "#{#this.1abc}|a name is expected at '1abc'"
    })
    void rejectsUnsupportedForms(String source, String reason) {
        // act & assert
        assertThatThrownBy(() -> RoutingExpressionParser.parse(source))
                .isInstanceOf(RoutingExpressionException.class)
                .hasMessage("Unsupported routing expression '" + source + "': " + reason + "; "
                        + RoutingExpressionParser.SUPPORTED_FORMS);
    }
}
