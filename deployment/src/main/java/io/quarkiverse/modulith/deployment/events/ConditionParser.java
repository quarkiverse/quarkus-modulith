package io.quarkiverse.modulith.deployment.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses the {@code condition} of an {@code @ApplicationModuleListener}. The accepted language is the subset of SpEL
 * Spring Modulith users write in listener conditions:
 * <ul>
 * <li>the event: {@code #event}, {@code event}, {@code #root.event}, {@code #root.args[0]}, {@code args[0]},
 * {@code #a0}, {@code #p0} or {@code #} followed by the parameter name</li>
 * <li>property paths on it ({@code #event.order.total}) and methods without parameters
 * ({@code #event.isUrgent()})</li>
 * <li>number, string ({@code 'text'} or {@code "text"}), boolean and {@code null} literals</li>
 * <li>comparisons {@code == != < <= > >=} and their word forms {@code eq ne lt le gt ge}</li>
 * <li>{@code && || !} and their word forms {@code and or not}, and parentheses</li>
 * </ul>
 * Anything else is rejected, so the build fails instead of the condition behaving differently at runtime.
 */
public final class ConditionParser {

    private static final Map<String, String> WORD_OPERATORS = Map.of("eq", "==", "ne", "!=", "lt", "<", "le", "<=",
            "gt", ">", "ge", ">=");
    private static final Set<String> COMPARISONS = Set.of("==", "!=", "<", "<=", ">", ">=");
    private static final Set<String> TWO_CHARACTER_OPERATORS = Set.of("==", "!=", "<=", ">=", "&&", "||");
    private static final String SINGLE_CHARACTER_OPERATORS = "<>!.()[]";

    private final List<String> tokens;
    private final String parameterName;
    private int position;

    private ConditionParser(List<String> tokens, String parameterName) {
        this.tokens = tokens;
        this.parameterName = parameterName;
    }

    /**
     * @param expression the condition
     * @param parameterName the name of the listener's event parameter
     * @throws IllegalArgumentException if the expression is not valid
     */
    public static ConditionNode parse(String expression, String parameterName) {
        ConditionParser parser = new ConditionParser(tokenize(expression), parameterName);
        ConditionNode node = parser.or();
        if (parser.position < parser.tokens.size()) {
            throw new IllegalArgumentException("Unexpected '" + parser.tokens.get(parser.position) + "'");
        }
        return node;
    }

    static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '\'' || c == '"') {
                i = string(expression, i, tokens);
            } else if (Character.isDigit(c)) {
                int start = i;
                while (i < expression.length()
                        && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(expression.substring(start, i));
            } else if (c == '#' || Character.isJavaIdentifierStart(c)) {
                int start = i++;
                while (i < expression.length() && Character.isJavaIdentifierPart(expression.charAt(i))) {
                    i++;
                }
                tokens.add(expression.substring(start, i));
            } else if (i + 1 < expression.length() && TWO_CHARACTER_OPERATORS.contains(expression.substring(i, i + 2))) {
                tokens.add(expression.substring(i, i + 2));
                i += 2;
            } else if (SINGLE_CHARACTER_OPERATORS.indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));
                i++;
            } else {
                throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + i);
            }
        }
        return tokens;
    }

    /**
     * Reads a quoted string, where a doubled quote stands for one quote, and adds it as a token starting with a quote.
     *
     * @return the position after the closing quote
     */
    private static int string(String expression, int start, List<String> tokens) {
        char quote = expression.charAt(start);
        StringBuilder text = new StringBuilder().append('\'');
        int i = start + 1;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (c == quote) {
                if (i + 1 < expression.length() && expression.charAt(i + 1) == quote) {
                    text.append(quote);
                    i += 2;
                    continue;
                }
                tokens.add(text.toString());
                return i + 1;
            }
            text.append(c);
            i++;
        }
        throw new IllegalArgumentException("Unterminated string starting at position " + start);
    }

    private ConditionNode or() {
        ConditionNode left = and();
        while (acceptWord("||", "or")) {
            left = new OrNode(left, and());
        }
        return left;
    }

    private ConditionNode and() {
        ConditionNode left = unary();
        while (acceptWord("&&", "and")) {
            left = new AndNode(left, unary());
        }
        return left;
    }

    private ConditionNode unary() {
        if (acceptWord("!", "not")) {
            return new NotNode(unary());
        }
        ConditionNode left = primary();
        String operator = peek().toLowerCase(Locale.ROOT);
        operator = WORD_OPERATORS.getOrDefault(operator, operator);
        if (COMPARISONS.contains(operator)) {
            position++;
            return new CompareNode(operator, left, primary());
        }
        return left;
    }

    private ConditionNode primary() {
        String token = next();
        if (token.equals("(")) {
            ConditionNode inner = or();
            expect(")");
            return inner;
        }
        if (token.startsWith("'")) {
            return new LiteralNode(LiteralKind.STRING, token.substring(1));
        }
        if (Character.isDigit(token.charAt(0))) {
            return new LiteralNode(LiteralKind.NUMBER, token);
        }
        String word = token.toLowerCase(Locale.ROOT);
        if (word.equals("true") || word.equals("false")) {
            return new LiteralNode(LiteralKind.BOOLEAN, word);
        }
        if (word.equals("null")) {
            return new LiteralNode(LiteralKind.NULL, "");
        }
        root(token);
        List<String> segments = new ArrayList<>();
        while (accept(".")) {
            String name = next();
            if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                throw new IllegalArgumentException("Expected a property name after '.' but found '" + name + "'");
            }
            if (accept("(")) {
                expect(")");
                name = name + "()";
            }
            segments.add(name);
        }
        return new PathNode(segments);
    }

    /**
     * Consumes the reference to the event.
     */
    private void root(String token) {
        if (token.equals("#root")) {
            expect(".");
            String member = next();
            if (member.equals("args")) {
                index();
            } else if (!member.equals("event")) {
                throw new IllegalArgumentException("Expected 'event' or 'args[0]' after '#root.' but found '" + member + "'");
            }
        } else if (token.equals("args")) {
            index();
        } else if (!(token.equals("#event") || token.equals("event") || token.equals("#a0") || token.equals("#p0")
                || token.equals("#" + parameterName))) {
            throw new IllegalArgumentException("Unknown reference '" + token
                    + "', use #event, #root.event, #root.args[0], #a0, #p0 or #" + parameterName);
        }
    }

    private void index() {
        expect("[");
        expect("0");
        expect("]");
    }

    private boolean acceptWord(String symbol, String word) {
        String token = peek();
        if (token.equals(symbol) || token.equalsIgnoreCase(word)) {
            position++;
            return true;
        }
        return false;
    }

    private boolean accept(String token) {
        if (peek().equals(token)) {
            position++;
            return true;
        }
        return false;
    }

    private void expect(String token) {
        String found = next();
        if (!found.equals(token)) {
            throw new IllegalArgumentException("Expected '" + token + "' but found '" + found + "'");
        }
    }

    private String peek() {
        return position < tokens.size() ? tokens.get(position) : "";
    }

    private String next() {
        if (position >= tokens.size()) {
            throw new IllegalArgumentException("Unexpected end of the condition");
        }
        return tokens.get(position++);
    }
}
