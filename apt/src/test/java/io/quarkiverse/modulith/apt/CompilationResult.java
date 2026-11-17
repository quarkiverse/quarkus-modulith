package io.quarkiverse.modulith.apt;

import java.util.List;

/**
 * The outcome of a test compilation.
 */
record CompilationResult(boolean success, List<String> notes, List<String> errors) {
}
