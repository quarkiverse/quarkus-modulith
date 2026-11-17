package io.quarkiverse.modulith.deployment.docs;

import java.util.List;
import java.util.Map;

/**
 * A field or method of a {@link GeneratedClass}.
 */
record Member(String name, String descriptor, String signature, List<String> annotations,
        Map<Integer, String> parameterAnnotations) {
}
