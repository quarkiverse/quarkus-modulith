package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the inside of one expression, between <code>{</code> or {@code #{} and <code>}</code>.
 */
final class ExpressionReader {

    private static final String SPRING_THIS = "#this";
    private static final String SPRING_ROOT = "#root";
    private static final String QUARKUS_EVENT = "event";
    private static final String QUARKUS_INJECT = "inject:";

    private final String source;
    private final String text;
    private final boolean spring;
    private int position;

    ExpressionReader(String source, String text, boolean spring) {
        this.source = source;
        this.text = text;
        this.spring = spring;
    }

    /**
     * @return the chain the expression consists of
     */
    ExpressionChain readExpression() {
        ExpressionChain chain = readChain(true);
        skipWhitespace();
        if (position < text.length()) {
            throw unsupported("unexpected '" + text.substring(position) + "'");
        }
        return chain;
    }

    private ExpressionChain readChain(boolean beansAllowed) {
        skipWhitespace();
        String beanName = readRoot(beansAllowed);
        List<ExpressionMember> members = new ArrayList<>();
        while (peek('.')) {
            position++;
            members.add(readMember(!beanName.isEmpty() && members.isEmpty()));
        }
        if (!beanName.isEmpty() && members.isEmpty()) {
            throw unsupported("the bean " + beanName + " must be followed by a method call");
        }
        return new ExpressionChain(beanName, members);
    }

    /**
     * @return the bean name, or an empty string for the event
     */
    private String readRoot(boolean beansAllowed) {
        String bean = spring ? "@" : QUARKUS_INJECT;
        if (text.startsWith(bean, position)) {
            if (!beansAllowed) {
                throw unsupported("method arguments must be the event or one of its properties");
            }
            position += bean.length();
            return readName();
        }
        if (spring) {
            if (text.startsWith(SPRING_THIS, position) || text.startsWith(SPRING_ROOT, position)) {
                position += SPRING_THIS.length();
                return "";
            }
            throw unsupported("an expression starts with #this, #root or @bean");
        }
        if (!readName().equals(QUARKUS_EVENT)) {
            throw unsupported("an expression starts with event or inject:bean");
        }
        return "";
    }

    private ExpressionMember readMember(boolean beanMethod) {
        String name = readName();
        if (!peek('(')) {
            if (beanMethod) {
                throw unsupported("the bean must be followed by a method call, not by the property " + name);
            }
            return ExpressionMember.property(name);
        }
        position++;
        List<ExpressionChain> arguments = new ArrayList<>();
        skipWhitespace();
        if (peek(')')) {
            position++;
        } else {
            do {
                arguments.add(readChain(false));
                skipWhitespace();
            } while (consume(','));
            if (!consume(')')) {
                throw unsupported("a method call has no closing )");
            }
        }
        if (!beanMethod && !arguments.isEmpty()) {
            throw unsupported("only methods of beans take arguments, " + name + " is called on the event");
        }
        return ExpressionMember.call(name, arguments);
    }

    private String readName() {
        skipWhitespace();
        int start = position;
        while (position < text.length() && isNamePart(text.charAt(position), position == start)) {
            position++;
        }
        if (start == position) {
            throw unsupported("a name is expected at '" + text.substring(start) + "'");
        }
        return text.substring(start, position);
    }

    private static boolean isNamePart(char character, boolean first) {
        return first ? Character.isJavaIdentifierStart(character) : Character.isJavaIdentifierPart(character);
    }

    private boolean peek(char expected) {
        skipWhitespace();
        return position < text.length() && text.charAt(position) == expected;
    }

    private boolean consume(char expected) {
        boolean found = peek(expected);
        if (found) {
            position++;
        }
        return found;
    }

    private void skipWhitespace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    private RoutingExpressionException unsupported(String reason) {
        return RoutingExpressionParser.unsupported(source, reason);
    }
}
