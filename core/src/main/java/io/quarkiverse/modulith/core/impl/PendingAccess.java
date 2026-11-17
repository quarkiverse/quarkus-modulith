package io.quarkiverse.modulith.core.impl;

import org.objectweb.asm.Type;

/**
 * A reference made by an instruction, before it is attributed to its code unit.
 *
 * @param verb the kind of reference, {@code calls method}
 * @param target the referenced type
 * @param targetDescription how the target is shown
 * @param line the source line, 0 if unknown
 */
record PendingAccess(String verb, Type target, String targetDescription, int line) {
}
