package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ConditionParserTest {

    static final PathNode EVENT = new PathNode(List.of());
    static final PathNode AMOUNT = new PathNode(List.of("amount"));

    @ParameterizedTest
    @MethodSource("expressions")
    void parsesTheSupportedSubsetOfSpel(String expression, ConditionNode expected) {
        // arrange
        String parameter = "order";

        // act
        ConditionNode parsed = ConditionParser.parse(expression, parameter);

        // assert
        assertThat(parsed).isEqualTo(expected);
    }

    static List<Object[]> expressions() {
        return List.of(
                new Object[] { "#event", EVENT },
                new Object[] { "event", EVENT },
                new Object[] { "#root.event", EVENT },
                new Object[] { "#root.args[0]", EVENT },
                new Object[] { "args[0]", EVENT },
                new Object[] { "#a0", EVENT },
                new Object[] { "#p0", EVENT },
                new Object[] { "#order.amount", AMOUNT },
                new Object[] { "#event.customer.isVip()", new PathNode(List.of("customer", "isVip()")) },
                new Object[] { "#event.amount > 10", new CompareNode(">", AMOUNT, number("10")) },
                new Object[] { "#event.amount GE 1.5", new CompareNode(">=", AMOUNT, number("1.5")) },
                new Object[] { "#event.amount lt 2", new CompareNode("<", AMOUNT, number("2")) },
                new Object[] { "#event.amount le 2", new CompareNode("<=", AMOUNT, number("2")) },
                new Object[] { "#event.amount gt 2", new CompareNode(">", AMOUNT, number("2")) },
                new Object[] { "#event.amount eq 2", new CompareNode("==", AMOUNT, number("2")) },
                new Object[] { "#event.amount ne 2", new CompareNode("!=", AMOUNT, number("2")) },
                new Object[] { "#event.amount != 2", new CompareNode("!=", AMOUNT, number("2")) },
                new Object[] { "#event.amount <= 2", new CompareNode("<=", AMOUNT, number("2")) },
                new Object[] { "#event.amount >= 2", new CompareNode(">=", AMOUNT, number("2")) },
                new Object[] { "#event.amount < 2", new CompareNode("<", AMOUNT, number("2")) },
                new Object[] { "#event.name == 'it''s'", new CompareNode("==", new PathNode(List.of("name")),
                        new LiteralNode(LiteralKind.STRING, "it's")) },
                new Object[] { "#event.name == \"say \"\"hi\"\"\"", new CompareNode("==", new PathNode(List.of("name")),
                        new LiteralNode(LiteralKind.STRING, "say \"hi\"")) },
                new Object[] { "TRUE", new LiteralNode(LiteralKind.BOOLEAN, "true") },
                new Object[] { "False", new LiteralNode(LiteralKind.BOOLEAN, "false") },
                new Object[] { "#event == NULL", new CompareNode("==", EVENT, new LiteralNode(LiteralKind.NULL, "")) },
                new Object[] { "!#event.open", new NotNode(new PathNode(List.of("open"))) },
                new Object[] { "NOT #event.open", new NotNode(new PathNode(List.of("open"))) },
                new Object[] { "#a0.x && #a0.y and #a0.z", new AndNode(new AndNode(path("x"), path("y")), path("z")) },
                new Object[] { "#a0.x || #a0.y OR #a0.z", new OrNode(new OrNode(path("x"), path("y")), path("z")) },
                new Object[] { "#a0.x or #a0.y and #a0.z", new OrNode(path("x"), new AndNode(path("y"), path("z"))) },
                new Object[] { " ( #a0.x or #a0.y ) and #a0.z ",
                        new AndNode(new OrNode(path("x"), path("y")), path("z")) });
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "#event.amount >|Unexpected end of the condition",
            "#event.amount > 1 1|Unexpected '1'",
            "#event.amount @ 1|Unexpected character '@' at position 14",
            "#event.name == 'open|Unterminated string starting at position 15",
            "#event.|Unexpected end of the condition",
            "#event.(|Expected a property name after '.' but found '('",
            "#event.size(|Unexpected end of the condition",
            "#event.size(1)|Expected ')' but found '1'",
            "#root.target|Expected 'event' or 'args[0]' after '#root.' but found 'target'",
            "#root|Unexpected end of the condition",
            "#root event|Expected '.' but found 'event'",
            "args[1]|Expected '0' but found '1'",
            "args.0|Expected '[' but found '.'",
            "args[0|Unexpected end of the condition",
            "#other.amount|Unknown reference '#other', use #event, #root.event, #root.args[0], #a0, #p0 or #order",
            "(#event.open|Unexpected end of the condition",
            "|Unexpected end of the condition"
    })
    void rejectsEverythingElseWithAReason(String expression, String message) {
        // arrange
        String source = Objects.requireNonNullElse(expression, "");

        // act & assert
        assertThatThrownBy(() -> ConditionParser.parse(source, "order")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    @Test
    void tokenizesOperatorsNumbersAndIdentifiers() {
        // arrange
        String expression = "#event.total>=10.25&&!flag";

        // act
        List<String> tokens = ConditionParser.tokenize(expression);

        // assert
        assertThat(tokens).containsExactly("#event", ".", "total", ">=", "10.25", "&&", "!", "flag");
    }

    static LiteralNode number(String text) {
        return new LiteralNode(LiteralKind.NUMBER, text);
    }

    static PathNode path(String segment) {
        return new PathNode(List.of(segment));
    }
}
