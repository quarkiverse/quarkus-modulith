package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses routing expressions of {@code Externalized} annotations. Two notations are accepted and mean the same:
 * Quarkus' <code>{event.lastname}</code> and <code>{inject:keys.of(event)}</code>, and the forms Spring Modulith
 * documents, {@code #{#this.getLastname()}} and {@code #{@keys.of(#this)}}. Expressions may be mixed with literal
 * text. Anything else fails the build.
 */
final class RoutingExpressionParser {

    static final String SUPPORTED_FORMS = "supported forms are {event}, {event.property}, {event.method()},"
            + " {inject:bean.method(event)} and Spring's #{#this}, #{#this.property}, #{#this.method()},"
            + " #{@bean.method(#this)}";

    private RoutingExpressionParser() {
    }

    /**
     * @param source an expression part of a routing target, such as <code>{event.id}</code> or
     *        {@code orders-#{#this.region}}
     * @throws RoutingExpressionException if the expression uses an unsupported form
     */
    static ExpressionTemplate parse(String source) {
        List<TemplateSegment> segments = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int position = 0;
        while (position < source.length()) {
            boolean spring = source.startsWith("#{", position);
            if (spring || source.charAt(position) == '{') {
                int start = position + (spring ? 2 : 1);
                int end = source.indexOf('}', start);
                if (end < 0) {
                    throw unsupported(source, "an expression has no closing }");
                }
                addLiteral(segments, literal);
                ExpressionReader reader = new ExpressionReader(source, source.substring(start, end), spring);
                segments.add(new ChainSegment(reader.readExpression()));
                position = end + 1;
            } else {
                literal.append(source.charAt(position));
                position++;
            }
        }
        addLiteral(segments, literal);
        return new ExpressionTemplate(source, segments);
    }

    private static void addLiteral(List<TemplateSegment> segments, StringBuilder literal) {
        if (!literal.isEmpty()) {
            segments.add(new LiteralSegment(literal.toString()));
            literal.setLength(0);
        }
    }

    static RoutingExpressionException unsupported(String source, String reason) {
        return new RoutingExpressionException(
                "Unsupported routing expression '" + source + "': " + reason + "; " + SUPPORTED_FORMS);
    }
}
