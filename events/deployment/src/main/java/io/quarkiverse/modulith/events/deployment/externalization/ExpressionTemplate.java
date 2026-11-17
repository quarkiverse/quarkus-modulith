package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.List;

/**
 * A parsed routing expression: literal text and expressions, evaluated against the event and concatenated.
 *
 * @param source the expression as declared
 * @param segments its parts, in order
 */
record ExpressionTemplate(String source, List<TemplateSegment> segments) {

    ExpressionTemplate {
        segments = List.copyOf(segments);
    }
}
