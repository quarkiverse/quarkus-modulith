package io.quarkiverse.modulith.core.impl;

/**
 * A reference from a scanned class to another class, as ArchUnit reports it.
 *
 * @param target the binary name of the referenced class
 * @param description where and how the reference is made, in ArchUnit's format
 */
record RawDependency(String target, String description) {
}
